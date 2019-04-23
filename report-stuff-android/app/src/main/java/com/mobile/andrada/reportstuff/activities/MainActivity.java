package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.mobile.andrada.reportstuff.R;

import static com.mobile.andrada.reportstuff.activities.ReportsListActivity.REPORTS_STATUS;

public class MainActivity extends AppCompatActivity {
    private static final int NEW_REPORTS = 1;
    private static final int ACTIVE_REPORTS = 2;
    private static final int CLOSED_REPORTS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button newReportsButton = findViewById(R.id.newReportsButton);
        newReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsListActivity.class);
                intent.putExtra(REPORTS_STATUS, "new");
                startActivityForResult(intent, NEW_REPORTS);
            }
        });

        Button openedReportsButton = findViewById(R.id.activeReportsButton);
        openedReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsListActivity.class);
                intent.putExtra(REPORTS_STATUS, "active");
                startActivityForResult(intent, ACTIVE_REPORTS);
            }
        });

        Button closedReportsButton = findViewById(R.id.closedReportsButton);
        closedReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReportsListActivity.class);
                intent.putExtra(REPORTS_STATUS, "closed");
                startActivityForResult(intent, CLOSED_REPORTS);
            }
        });
    }
}
