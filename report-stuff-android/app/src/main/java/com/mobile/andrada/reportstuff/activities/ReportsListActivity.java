package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ViewSwitcher;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.adapters.ReportAdapter;
import com.mobile.andrada.reportstuff.firestore.Report;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mobile.andrada.reportstuff.utils.LocationHelper.checkForLocationPermission;

public class ReportsListActivity extends AppCompatActivity implements
        ReportAdapter.OnItemClickListener, OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {
    private static final int ENTER_CHAT = 1;
    public static final String REPORTS_STATUS = "reports_status";
    public final static String TAG = "ReportsListActivity";
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseFirestore mFirestore;
    private ReportAdapter mAdapter;
    private Query mQuery;
    private String mReportsStatus;

    private FusedLocationProviderClient fusedLocationClient;

    private boolean mListViewVisibility = true;
    private boolean mMapViewVisibility = false;

    @BindView(R.id.reportRecyclerView)
    RecyclerView mReportsRecyclerView;

    GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_list);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Reports");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();

        mReportsStatus = getIntent().getStringExtra(REPORTS_STATUS);
        mQuery = mFirestore.collection("reports");

        String email = mFirebaseUser.getEmail();
        //TODO: mReportsCollectionReference
        switch (mReportsStatus) {
            case "new":
                mQuery = mQuery.whereEqualTo("status", "open")
                        .whereArrayContains("notifiedOfficials", email);
                break;
            case "active":
                mQuery = mQuery.whereEqualTo("status", "open")
                        .whereArrayContains("activeOfficials", email);
                break;
            case "closed":
                mQuery = mQuery.whereEqualTo("status", "closed")
                        .whereArrayContains("activeOfficials", email);
                break;
            default:
                mQuery = mQuery.whereArrayContains("activeOfficials", email);
                break;
        }

        mAdapter = new ReportAdapter(mQuery, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    mReportsRecyclerView.setVisibility(View.GONE);
                } else {
                    mReportsRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                // Show a snackbar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.reportsMapView);
        mapFragment.getMapAsync(this);
//        mMapView.onCreate(savedInstanceState);
//        mMapView.getMapAsync(this);

        mReportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mReportsRecyclerView.setHasFixedSize(true);
        mReportsRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.report_list_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem listView = menu.findItem(R.id.list_view);
        MenuItem mapView = menu.findItem(R.id.map_view);

        if (mListViewVisibility) {
            mListViewVisibility = false;
            mMapViewVisibility = true;
        } else {
            mListViewVisibility = true;
            mMapViewVisibility = false;
        }

        listView.setVisible(mListViewVisibility);
        mapView.setVisible(mMapViewVisibility);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, MainActivity.class));
                return true;

            case R.id.map_view:
                invalidateOptionsMenu();
                switchViews();
                return true;

            case R.id.list_view:
                invalidateOptionsMenu();
                switchViews();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (!checkForLocationPermission(this))
            return;
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnInfoWindowClickListener(this);

        // Zoom level between 2.0 and 21.0
        float zoomLevel = 14.7f;

        // Zoom into user's current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> mGoogleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        ), zoomLevel))
                );

        // Add reports as markers on map
        mQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && querySnapshot.isEmpty())
                    Log.i(TAG, "No reports found for this map view.");

                querySnapshot.forEach(doc -> {
                    Report report = doc.toObject(Report.class);
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(
                                    report.getLatestLocation().getLatitude(),
                                    report.getLatestLocation().getLongitude()
                            )).title(report.getCitizenName())
                            .snippet(report.getLatestTime().toString()));
                    marker.setTag(doc.getId());
                    marker.showInfoWindow();
                });
            } else {
                Log.e(TAG, "Error when querying firestore: " + task.getException());
            }
        });
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.i(TAG, "Info window clicked");
        String reportId = (String) marker.getTag();

        openChatForReport(reportId);
    }

    private void switchViews() {
        ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
        FrameLayout mapView = findViewById(R.id.reportsMapView);
        RecyclerView listView = findViewById(R.id.reportRecyclerView);

        if (viewSwitcher.getCurrentView() != listView) {
            viewSwitcher.showPrevious();
        } else if (viewSwitcher.getCurrentView() != mapView) {
            viewSwitcher.showNext();
        }
    }

    @Override
    public void onPause() {
        mAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    @Override
    public void onItemClick(Report report) {
        openChatForReport(report.getRid());
    }

    public void openChatForReport(String reportID) {
        Intent intent = new Intent(ReportsListActivity.this, ChatActivity.class);
        intent.putExtra(ChatActivity.REPORT_ID, reportID);
        intent.putExtra(REPORTS_STATUS, mReportsStatus);
        startActivityForResult(intent, ENTER_CHAT);
    }
}
