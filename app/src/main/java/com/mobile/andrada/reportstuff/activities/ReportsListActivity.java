package com.mobile.andrada.reportstuff.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.mobile.andrada.reportstuff.R;
import com.mobile.andrada.reportstuff.adapter.ReportsRecyclerViewAdapter;
import com.mobile.andrada.reportstuff.db.Report;
import com.mobile.andrada.reportstuff.model.ReportsViewModel;

import java.util.List;

public class ReportsListActivity extends AppCompatActivity {
    private ReportsViewModel reportsViewModel;
    private ReportsRecyclerViewAdapter reportsRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_list);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Reports");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        reportsRecyclerViewAdapter = new ReportsRecyclerViewAdapter();

        RecyclerView recyclerView = findViewById(R.id.reportRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(reportsRecyclerViewAdapter);

        reportsViewModel = ViewModelProviders.of(this).get(ReportsViewModel.class);
        reportsViewModel.getReports().observe(this, new Observer<List<Report>>() {
            @Override
            public void onChanged(@Nullable List<Report> reports) {
                reportsRecyclerViewAdapter.setData(reports);
            }
        });

        reportsRecyclerViewAdapter.setOnClickListener(new ReportsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Report report) {
                Intent intent = new Intent(ReportsListActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_ID, report.getId());
                startActivity(intent);
            }
        });
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
}
