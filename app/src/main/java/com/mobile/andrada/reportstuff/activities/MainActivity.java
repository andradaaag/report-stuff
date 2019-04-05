package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.mobile.andrada.reportstuff.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button newReportsButton = findViewById(R.id.newReportsButton);
        newReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsListActivity.class));
            }
        });

        Button openedReportsButton = findViewById(R.id.openedReportsButton);
        openedReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsListActivity.class));
            }
        });

        Button closedReportsButton = findViewById(R.id.closedReportsButton);
        closedReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportsListActivity.class));
            }
        });
    }
}
