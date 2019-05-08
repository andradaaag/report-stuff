package com.mobile.andrada.reportstuff.firestore;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.List;

public class Report {
    private List<String> activeUsers;
    private String citizenName;
    private GeoPoint latestLocation;
    private Date latestTime;
    private List<String> openedBy;
    private String status;
    private String rid;

    public Report() {
    }

    public Report(List<String> activeUsers, String citizenName, GeoPoint latestLocation, Date latestTime, List<String> openedBy, String status) {
        this.activeUsers = activeUsers;
        this.citizenName = citizenName;
        this.latestLocation = latestLocation;
        this.latestTime = latestTime;
        this.openedBy = openedBy;
        this.status = status;
    }

    public List<String> getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(List<String> activeUsers) {
        this.activeUsers = activeUsers;
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

    public List<String> getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(List<String> openedBy) {
        this.openedBy = openedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Report{" +
                "activeUsers=" + activeUsers +
                ", citizenName='" + citizenName + '\'' +
                ", latestLocation='" + latestLocation + '\'' +
                ", latestTime=" + latestTime +
                ", openedBy=" + openedBy +
                ", status='" + status + '\'' +
                '}';
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }
}
