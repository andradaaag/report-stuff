package com.mobile.andrada.reportstuff.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

@Database(entities = {Report.class}, version = 1, exportSchema = false)
abstract class ReportsDatabase extends RoomDatabase {
    abstract ReportDao reportDao();

    private static volatile ReportsDatabase INSTANCE;

    static ReportsDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ReportsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ReportsDatabase.class, "reports_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };

    public static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {
        private final ReportDao mDao;

        PopulateDbAsync(ReportsDatabase db) {
            mDao = db.reportDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mDao.deleteAll();

            Report report = new Report(
                    "76B Marbles Street",
                    "Megan Miller",
                    "10 mins ago"
            );
            report.setId(Long.parseLong("1"));
            mDao.insert(report);
            return null;
        }
    }
}
