package com.damors.zuji.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 足迹数据仓库类
 * 作为数据源和ViewModel之间的中间层
 */
public class FootprintRepository {

    private final FootprintDao footprintDao;
    private final LiveData<List<FootprintEntity>> allFootprints;
    private final ExecutorService executorService;

    /**
     * 构造函数，初始化数据库和DAO
     * @param application 应用实例
     */
    public FootprintRepository(Application application) {
        FootprintDatabase database = FootprintDatabase.getInstance(application);
        footprintDao = database.footprintDao();
        allFootprints = footprintDao.getAllFootprints();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 获取所有足迹记录
     * @return 足迹列表的LiveData
     */
    public LiveData<List<FootprintEntity>> getAllFootprints() {
        return allFootprints;
    }
    
    /**
     * 获取按时间戳降序排列的所有足迹记录
     * @return 按时间戳降序排列的足迹列表的LiveData
     */
    public LiveData<List<FootprintEntity>> getAllFootprintsByTimestampDesc() {
        return footprintDao.getAllFootprintsByTimestampDesc();
    }
    
    /**
     * 根据ID获取足迹记录
     * @param id 足迹ID
     * @return 足迹实体的LiveData
     */
    public LiveData<FootprintEntity> getFootprintById(int id) {
        return footprintDao.getFootprintById(id);
    }

    /**
     * 根据时间范围获取足迹记录
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 足迹列表的LiveData
     */
    public LiveData<List<FootprintEntity>> getFootprintsByTimeRange(long startTime, long endTime) {
        return footprintDao.getFootprintsByTimeRange(startTime, endTime);
    }

    /**
     * 插入足迹记录
     * @param footprint 足迹实体
     */
    public void insert(FootprintEntity footprint) {
        executorService.execute(() -> footprintDao.insert(footprint));
    }

    /**
     * 更新足迹记录
     * @param footprint 足迹实体
     */
    public void update(FootprintEntity footprint) {
        executorService.execute(() -> footprintDao.update(footprint));
    }

    /**
     * 删除足迹记录
     * @param footprint 足迹实体
     */
    public void delete(FootprintEntity footprint) {
        executorService.execute(() -> footprintDao.delete(footprint));
    }

    /**
     * 删除所有足迹记录
     */
    public void deleteAllFootprints() {
        executorService.execute(footprintDao::deleteAllFootprints);
    }
}