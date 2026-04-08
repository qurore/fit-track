package com.github.qurore.fittrack.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.github.qurore.fittrack.data.dao.TimerStateDao;
import com.github.qurore.fittrack.data.entity.TimerStateEntity;

@Database(entities = {TimerStateEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TimerStateDao timerStateDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "fittrack_timer_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
