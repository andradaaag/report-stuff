package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.firestore.Report;

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

    private String mReportID;

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

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();

        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            mFirebaseUser.getIdToken(true).addOnSuccessListener(result -> {
                boolean isPoliceman = result.getClaims().containsKey("policeman");
                boolean isFirefighter = result.getClaims().containsKey("firefighter");
                boolean isSmurd = result.getClaims().containsKey("smurd");

                boolean isOfficial = isPoliceman || isFirefighter || isSmurd;
                if (isOfficial) {
                    showOfficialUI();
                } else {
                    showCitizenUI();
                }
            });
        }
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
        FirebaseFirestore.setLoggingEnabled(true);
        mFirestore = FirebaseFirestore.getInstance();

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
}
