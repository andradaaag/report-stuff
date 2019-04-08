package com.mobile.andrada.reportstuff.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface ReportDao {
    @Insert
    void insert(Report report);

    @Insert
    void insertAll(List<Report> reports);

    @Query("SELECT * FROM reports WHERE reportID = :reportID")
    Report getReport(Long reportID);

    @Update
    void update(Report report);

    @Delete
    void delete(Report report);

    @Query("DELETE FROM reports")
    void deleteAll();

    @Query("SELECT * FROM reports")
    LiveData<List<Report>> getReports();
}
