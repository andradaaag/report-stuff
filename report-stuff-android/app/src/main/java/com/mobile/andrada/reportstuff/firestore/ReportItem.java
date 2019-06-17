package com.mobile.andrada.reportstuff.firestore;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.List;

public class ReportItem extends Report {
    private String rid;

    public ReportItem(Report report) {
        super(report.getActiveOfficials(), report.getCitizenName(), report.getCitizenEmail(), report.getLatestLocation(), report.getLatestTime(), report.getNotifiedOfficials(), report.getStatus());
    }

    public ReportItem(List<String> activeOfficials, String citizenName, String citizenEmail, GeoPoint latestLocation, Date latestTime, List<String> notifiedOfficials, String status) {
        super(activeOfficials, citizenName, citizenEmail, latestLocation, latestTime, notifiedOfficials, status);
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    @Override
    public String toString() {
        return "ReportItem{" + super.toString() +
                "rid='" + rid + '\'' +
                '}';
    }
}
