package com.mobile.andrada.reportstuff.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.firestore.OfficialRecord;
import com.mobile.andrada.reportstuff.firestore.Report;
import com.mobile.andrada.reportstuff.utils.Utils.*;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mobile.andrada.reportstuff.activities.ChatActivity.REPORT_ID;
import static com.mobile.andrada.reportstuff.activities.ReportsListActivity.REPORTS_STATUS;

public class MainActivity extends AppCompatActivity {
    private static final int NEW_REPORTS = 1;
    private static final int ACTIVE_REPORTS = 2;
    private static final int CLOSED_REPORTS = 3;
    private static final int ENTER_CHAT = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;

    private static final long UPDATE_INTERVAL = 10 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;
    private static final String TAG = "MainActivity";

    private Role mRole;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private String mReportID;
    private String mUid;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseFirestore mFirestore;

    @BindView(R.id.newReportsButton)
    Button newReportsButton;

    @BindView(R.id.activeReportsButton)
    Button activeReportsButton;

    @BindView(R.id.closedReportsButton)
    Button closedReportsButton;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Initialize Firestore
        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            mUid = mFirebaseUser.getUid();
            mFirebaseUser.getIdToken(true).addOnSuccessListener(result -> {
                mRole = Role.citizen;
                if (result.getClaims().containsKey("policeman"))
                    mRole = Role.policeman;
                else if (result.getClaims().containsKey("firefighter"))
                    mRole = Role.firefighter;
                else if (result.getClaims().containsKey("smurd"))
                    mRole = Role.smurd;

                if (mRole == Role.citizen) {
                    showCitizenUI();
                } else {
                    showOfficialUI();
                    startSendingLocationToFirestore();
                    startLocationUpdates();
                }
            });
        }
    }

    private void startSendingLocationToFirestore() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                sendLocationToFirestore(location);
            }
        };
    }

    private void sendLocationToFirestore(Location location) {
        CollectionReference officials = mFirestore.collection("officials");
        officials.whereEqualTo("officialId", mUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                        GeoPoint locationGP = new GeoPoint(location.getLatitude(), location.getLongitude());
                        if (snapshots.size() > 0) {
                            // Update existing official record
                            officials.document(snapshots.get(0).getId()).update("location", locationGP);
                        } else {
                            // Create new official record
                            officials.add(new OfficialRecord("", locationGP, mUid, mRole.toString()));
                        }
                    }
                });
    }

    private void showOfficialUI() {
        newReportsButton.setVisibility(Button.VISIBLE);
        newReportsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportsListActivity.class);
            intent.putExtra(REPORTS_STATUS, "new");
            startActivityForResult(intent, NEW_REPORTS);
        });

        activeReportsButton.setVisibility(Button.VISIBLE);
        activeReportsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportsListActivity.class);
            intent.putExtra(REPORTS_STATUS, "active");
            startActivityForResult(intent, ACTIVE_REPORTS);
        });

        closedReportsButton.setVisibility(Button.VISIBLE);
        closedReportsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportsListActivity.class);
            intent.putExtra(REPORTS_STATUS, "closed");
            startActivityForResult(intent, CLOSED_REPORTS);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showCitizenUI() {
        // Check if citizen has active report
        mFirestore.collection("reports")
                .whereArrayContains("activeUsers", mFirebaseUser.getEmail())
                .limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> reports = task.getResult().getDocuments();
                reports.removeIf(report -> report.get("status").equals("closed"));
                if (reports.size() > 0) {
                    // If citizen has report, open chat
                    mReportID = reports.get(0).getId();
                    openChat();
                } else {
                    // There is no report for this citizen, create a new one
                    Report report = new Report(
                            Collections.singletonList(mFirebaseUser.getEmail()),
                            mFirebaseUser.getDisplayName(),
                            null,
                            Calendar.getInstance().getTime(),
                            null,
                            "new"
                    );
                    mFirestore.collection("reports").add(report).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DocumentReference reportReference = task1.getResult();
                            mReportID = reportReference.getId();
                            openChat();
                        }
                    });
                }
            }
        });
    }

    private void openChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(REPORT_ID, mReportID);
        startActivityForResult(intent, ENTER_CHAT);
        finish();
    }

    public void checkForLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_LOCATION) {
            if (grantResults.length == 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Permissions for location needed in order to automatically send it to rescuers.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(UPDATE_INTERVAL);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        locationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }

    private void startLocationUpdates() {
        checkForLocationPermission();
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        startLocationUpdates();
    }
}
