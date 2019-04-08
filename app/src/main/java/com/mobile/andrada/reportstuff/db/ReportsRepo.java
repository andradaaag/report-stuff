package com.mobile.andrada.reportstuff.db;

import android.app.Application;
import android.arch.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class ReportsRepo {
    private ReportDao reportDao;
    private LiveData<List<Report>> reports;

    public ReportsRepo(Application application) {
        ReportsDatabase reportsDatabase = ReportsDatabase.getDatabase(application);
        reportDao = reportsDatabase.reportDao();
        reports = reportDao.getReports();
    }

    public LiveData<List<Report>> getReports() {
        return reports;
    }
}