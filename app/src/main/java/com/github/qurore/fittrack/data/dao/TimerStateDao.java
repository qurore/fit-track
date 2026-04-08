package com.github.qurore.fittrack.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.github.qurore.fittrack.data.entity.TimerStateEntity;

@Dao
public interface TimerStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(TimerStateEntity entity);

    @Query("SELECT * FROM timer_state WHERE timerType = :type LIMIT 1")
    TimerStateEntity getByType(String type);

    @Query("DELETE FROM timer_state WHERE timerType = :type")
    void deleteByType(String type);

    @Query("DELETE FROM timer_state")
    void deleteAll();
}
