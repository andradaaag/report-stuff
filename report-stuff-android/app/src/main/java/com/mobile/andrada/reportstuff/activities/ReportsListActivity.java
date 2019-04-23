package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.adapters.ReportAdapter;
import com.mobile.andrada.reportstuff.firestore.Report;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReportsListActivity extends AppCompatActivity implements
        ReportAdapter.OnItemClickListener {
    private static final int ENTER_CHAT = 1;
    public static final String REPORTS_STATUS = "reports_status";
    public final static String TAG = "ReportsListActivity";
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseFirestore mFirestore;
    private ReportAdapter mAdapter;
    private Query mQuery;
    private String mReportsStatus;

    @BindView(R.id.reportRecyclerView)
    RecyclerView mReportsRecyclerView;

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
        mQuery = mFirestore.collection("reports")
                .whereEqualTo("status", mReportsStatus);

        if(!mReportsStatus.equals("new")){
            mQuery = mQuery.whereArrayContains("activeUsers", mFirebaseUser.getEmail());
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

        mReportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mReportsRecyclerView.setHasFixedSize(true);
        mReportsRecyclerView.setAdapter(mAdapter);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        Intent intent = new Intent(ReportsListActivity.this, ChatActivity.class);
        intent.putExtra(ChatActivity.REPORT_ID, report.getRid());
        intent.putExtra(REPORTS_STATUS, mReportsStatus);
        startActivityForResult(intent, ENTER_CHAT);
    }
}
