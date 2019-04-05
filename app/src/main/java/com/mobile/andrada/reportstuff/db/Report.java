package com.mobile.andrada.reportstuff.db;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "reports")
public class Report {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "reportID")
    private Long id;
    private String location;
    private String citizenName;
    private String date;

    public Report(String location, String citizenName, String date) {
        this.location = location;
        this.citizenName = citizenName;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", location='" + location + '\'' +
                ", citizenName='" + citizenName + '\'' +
                ", date=" + date +
                '}';
    }
}