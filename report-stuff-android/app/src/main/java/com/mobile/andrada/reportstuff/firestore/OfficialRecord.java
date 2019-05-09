package com.mobile.andrada.reportstuff.firestore;

import com.google.firebase.firestore.GeoPoint;

public class OfficialRecord {
    private String email;
    private String fcmToken;
    private GeoPoint location;
    private String officialId;
    private String role;

    public OfficialRecord() {
    }

    public OfficialRecord(String email, String fcmToken, GeoPoint location, String officialId, String role) {
        this.email = email;
        this.fcmToken = fcmToken;
        this.location = location;
        this.officialId = officialId;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getOfficialId() {
        return officialId;
    }

    public void setOfficialId(String officialId) {
        this.officialId = officialId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "OfficialRecord{" +
                "email='" + email + '\'' +
                ", fcmToken='" + fcmToken + '\'' +
                ", location=" + location +
                ", officialId='" + officialId + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
