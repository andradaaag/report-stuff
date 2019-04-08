package com.mobile.andrada.reportstuff.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.mobile.andrada.reportstuff.db.Report;
import com.mobile.andrada.reportstuff.db.ReportsRepo;

import java.util.List;

public class ReportsViewModel extends AndroidViewModel {
    private ReportsRepo reportsRepo;
    private LiveData<List<Report>> reports;

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        reportsRepo = new ReportsRepo(application);
        reports = reportsRepo.getReports();
    }

    public LiveData<List<Report>> getReports() {
        return reports;
    }
}