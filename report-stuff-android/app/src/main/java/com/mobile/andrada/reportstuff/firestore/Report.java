package com.mobile.andrada.reportstuff.firestore;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.List;

public class Report {
    private List<String> activeOfficials;
    private String citizenName;
    private String citizenEmail;
    private GeoPoint latestLocation;
    private Date latestTime;
    private List<String> notifiedOfficials;
    private String status;

    public Report() {
    }

    public Report(List<String> activeOfficials, String citizenName, String citizenEmail, GeoPoint latestLocation, Date latestTime, List<String> notifiedOfficials, String status) {
        this.activeOfficials = activeOfficials;
        this.citizenName = citizenName;
        this.citizenEmail = citizenEmail;
        this.latestLocation = latestLocation;
        this.latestTime = latestTime;
        this.notifiedOfficials = notifiedOfficials;
        this.status = status;
    }

    public List<String> getActiveOfficials() {
        return activeOfficials;
    }

    public void setActiveOfficials(List<String> activeOfficials) {
        this.activeOfficials = activeOfficials;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public GeoPoint getLatestLocation() {
        return latestLocation;
    }

    public void setLatestLocation(GeoPoint latestLocation) {
        this.latestLocation = latestLocation;
    }

    public Date getLatestTime() {
        return latestTime;
    }

    public void setLatestTime(Date latestTime) {
        this.latestTime = latestTime;
    }

    public List<String> getNotifiedOfficials() {
        return notifiedOfficials;
    }

    public void setNotifiedOfficials(List<String> notifiedOfficials) {
        this.notifiedOfficials = notifiedOfficials;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCitizenEmail() {
        return citizenEmail;
    }

    public void setCitizenEmail(String citizenEmail) {
        this.citizenEmail = citizenEmail;
    }

    @Override
    public String toString() {
        return "Report{" +
                "activeOfficials=" + activeOfficials +
                ", citizenName='" + citizenName + '\'' +
                ", citizenEmail='" + citizenEmail + '\'' +
                ", latestLocation=" + latestLocation +
                ", latestTime=" + latestTime +
                ", notifiedOfficials=" + notifiedOfficials +
                ", status='" + status + '\'' +
                '}';
    }
}
