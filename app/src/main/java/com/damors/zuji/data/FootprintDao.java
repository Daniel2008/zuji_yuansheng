package com.damors.zuji.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 足迹数据访问对象接口
 * 定义了对足迹数据的各种操作方法
 */
@Dao
public interface FootprintDao {

    /**
     * 插入一条足迹记录
     * @param footprint 足迹实体
     */
    @Insert
    void insert(FootprintEntity footprint);

    /**
     * 更新足迹记录
     * @param footprint 足迹实体
     */
    @Update
    void update(FootprintEntity footprint);

    /**
     * 删除足迹记录
     * @param footprint 足迹实体
     */
    @Delete
    void delete(FootprintEntity footprint);

    /**
     * 获取所有足迹记录，按时间戳降序排列
     * @return 足迹列表的LiveData
     */
    @Query("SELECT * FROM footprints ORDER BY timestamp DESC")
    LiveData<List<FootprintEntity>> getAllFootprints();
    
    /**
     * 获取所有足迹记录，明确按时间戳降序排列
     * @return 足迹列表的LiveData
     */
    @Query("SELECT * FROM footprints ORDER BY timestamp DESC")
    LiveData<List<FootprintEntity>> getAllFootprintsByTimestampDesc();

    /**
     * 根据ID获取足迹记录
     * @param id 足迹ID
     * @return 足迹实体的LiveData
     */
    @Query("SELECT * FROM footprints WHERE id = :id")
    LiveData<FootprintEntity> getFootprintById(int id);

    /**
     * 获取指定日期范围内的足迹记录
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 足迹列表的LiveData
     */
    @Query("SELECT * FROM footprints WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    LiveData<List<FootprintEntity>> getFootprintsByTimeRange(long startTime, long endTime);

    /**
     * 删除所有足迹记录
     */
    @Query("DELETE FROM footprints")
    void deleteAllFootprints();
}