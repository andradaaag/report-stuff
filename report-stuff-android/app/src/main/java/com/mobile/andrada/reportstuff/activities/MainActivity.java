package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.firestore.OfficialRecord;
import com.mobile.andrada.reportstuff.firestore.Report;
import com.mobile.andrada.reportstuff.utils.Utils.Role;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mobile.andrada.reportstuff.activities.ChatActivity.IS_OFFICIAL;
import static com.mobile.andrada.reportstuff.activities.ChatActivity.REPORT_ID;
import static com.mobile.andrada.reportstuff.activities.ChatActivity.STATUS_OPEN;
import static com.mobile.andrada.reportstuff.activities.ReportsListActivity.REPORTS_STATUS;
import static com.mobile.andrada.reportstuff.utils.LocationHelper.checkForLocationPermission;
import static com.mobile.andrada.reportstuff.utils.LocationHelper.convertLocation;

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

    @BindView(R.id.activeReportsButton)
    Button activeReportsButton;

    @BindView(R.id.askForHelpButton)
    Button askForHelpButton;

    @BindView(R.id.closedReportsButton)
    Button closedReportsButton;

    @BindView(R.id.newReportsButton)
    Button newReportsButton;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            mUid = mFirebaseUser.getUid();
            mFirebaseUser.getIdToken(false).addOnSuccessListener(result -> {
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
                    startSendingOfficialLocationToFirestore();
                    startLocationUpdates();
                }
            }).addOnFailureListener(exception -> Log.e(TAG, exception.getMessage()));
        }
    }

    private void startSendingOfficialLocationToFirestore() {
        createLocationRequest();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                sendOfficialLocationAndTokenToFirestore(location);
            }
        };
    }

    private void sendOfficialLocationAndTokenToFirestore(Location location) {
        SharedPreferences preferences = getSharedPreferences("FCM_TOKEN", MODE_PRIVATE);
        String token = preferences.getString("token", "");

        CollectionReference officials = mFirestore.collection("officials");
        officials.whereEqualTo("officialId", mUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                        if (snapshots.size() > 0) {
                            officials.document(snapshots.get(0).getId()).update(
                                    "location", convertLocation(location),
                                    "fcmToken", token
                            );
                        } else {
                            officials.add(new OfficialRecord(mFirebaseUser.getEmail(), token, convertLocation(location), mUid, mRole.toString()));
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

    private void showCitizenUI() {
        askForHelpButton.setVisibility(Button.VISIBLE);
        askForHelpButton.setOnClickListener(v -> askForHelpOnClick());
    }

    private void askForHelpOnClick() {
        // Check if citizen has active report and either create one or directly open chat
        mFirestore.collection("reports")
                .whereEqualTo("citizenEmail", mFirebaseUser.getEmail())
                .whereEqualTo("status", "open")
                .get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> reports = task.getResult().getDocuments();
                if (reports.size() > 0) {
                    mReportID = reports.get(0).getId();
                    openChat();
                } else {
                    createReportForCitizen();
                }
            } else {
                Log.e(TAG, "Error: " + task.getException());
            }
        });
    }

    private void createReportForCitizen() {
        if (!checkForLocationPermission(this)) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    Report report = new Report(
                            null,
                            mFirebaseUser.getDisplayName(),
                            mFirebaseUser.getEmail(),
                            convertLocation(location),
                            Calendar.getInstance().getTime(),
                            null,
                            "open"
                    );
                    mFirestore.collection("reports").add(report).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DocumentReference reportReference = task1.getResult();
                            mReportID = reportReference.getId();
                            openChat();
                        }
                    });
                });
    }

    private void openChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(REPORT_ID, mReportID);
        intent.putExtra(IS_OFFICIAL, false);
        intent.putExtra(REPORTS_STATUS, STATUS_OPEN);
        startActivityForResult(intent, ENTER_CHAT);
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
            if (mRole != null && mRole == Role.citizen) {
                createReportForCitizen();
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
        checkForLocationPermission(this);
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENTER_CHAT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Returned from chat");
            }
        }
    }
}
