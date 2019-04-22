package com.mobile.andrada.reportstuff.firestore;

import java.util.Date;

public class Message {
    private String location;
    private String mediaType;
    private String mediaUrl;
    private String name;
    private String photoUrl;
    private String text;
    private Date time;

    public Message() {
    }

    public Message(String location, String mediaType, String mediaUrl, String name, String photoUrl, String text, Date time) {
        this.location = location;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
        this.name = name;
        this.photoUrl = photoUrl;
        this.text = text;
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Message{" +
                "location='" + location + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", name='" + name + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                ", text='" + text + '\'' +
                ", time=" + time +
                '}';
    }
}
