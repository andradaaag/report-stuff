package com.mobile.andrada.reportstuff.firestore;

import java.util.Date;
import java.util.List;

public class Report {
    private List<String> activeUsers;
    private String lastLocation;
    private Date lastTime;
    private List<String> openedBy;
    private String status;

    public Report() {
    }

    public Report(List<String> activeUsers, String lastLocation, Date lastTime, List<String> openedBy, String status) {
        this.activeUsers = activeUsers;
        this.lastLocation = lastLocation;
        this.lastTime = lastTime;
        this.openedBy = openedBy;
        this.status = status;
    }

    public List<String> getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(List<String> activeUsers) {
        this.activeUsers = activeUsers;
    }

    public String getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(String lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
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
                ", lastLocation='" + lastLocation + '\'' +
                ", lastTime=" + lastTime +
                ", openedBy=" + openedBy +
                ", status='" + status + '\'' +
                '}';
    }
}
