package com.mobile.andrada.reportstuff.db;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "messages")
public class ChatMessage {
    @PrimaryKey
    @ColumnInfo(name = "messageID")
    private String id;
    private String location;
    private String name;
    private String date;
    private String text;
    private String photoUrl;
    private String imageUrl;

    public ChatMessage() {
    }

    public ChatMessage(String location, String name, String date, String text, String photoUrl, String imageUrl) {
        this.location = location;
        this.name = name;
        this.date = date;
        this.text = text;
        this.photoUrl = photoUrl;
        this.imageUrl = imageUrl;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
