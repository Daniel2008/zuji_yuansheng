package com.damors.zuji.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.data.FootprintRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 足迹ViewModel类
 * 作为UI和Repository之间的连接器
 */
public class FootprintViewModel extends AndroidViewModel {

    private final FootprintRepository repository;
    private final LiveData<List<FootprintEntity>> allFootprints;
    private LiveData<List<FootprintEntity>> allFootprintsByTimestampDesc;
    private MutableLiveData<Integer> cityCount = new MutableLiveData<>(0);

    /**
     * 构造函数
     * @param application 应用实例
     */
    public FootprintViewModel(@NonNull Application application) {
        super(application);
        repository = new FootprintRepository(application);
        allFootprints = repository.getAllFootprints();
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
        if (allFootprintsByTimestampDesc == null) {
            allFootprintsByTimestampDesc = repository.getAllFootprintsByTimestampDesc();
        }
        return allFootprintsByTimestampDesc;
    }
    
    /**
     * 获取足迹记录总数
     * @return 足迹记录总数
     */
    public int getFootprintCount() {
        if (allFootprints.getValue() != null) {
            return allFootprints.getValue().size();
        }
        return 0;
    }
    
    /**
     * 获取城市数量
     * @return 城市数量的LiveData
     */
    public LiveData<Integer> getCityCount() {
        updateCityCount();
        return cityCount;
    }
    
    /**
     * 更新城市数量统计
     */
    private void updateCityCount() {
        if (allFootprints.getValue() != null) {
            Set<String> cities = new HashSet<>();
            for (FootprintEntity footprint : allFootprints.getValue()) {
                if (footprint.getCityName() != null && !footprint.getCityName().isEmpty()) {
                    cities.add(footprint.getCityName());
                }
            }
            cityCount.setValue(cities.size());
        } else {
            cityCount.setValue(0);
        }
    }
    
    /**
     * 根据ID获取足迹记录
     * @param id 足迹ID
     * @return 足迹实体的LiveData
     */
    public LiveData<FootprintEntity> getFootprintById(int id) {
        return repository.getFootprintById(id);
    }

    /**
     * 根据时间范围获取足迹记录
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 足迹列表的LiveData
     */
    public LiveData<List<FootprintEntity>> getFootprintsByTimeRange(long startTime, long endTime) {
        return repository.getFootprintsByTimeRange(startTime, endTime);
    }

    /**
     * 插入足迹记录
     * @param footprint 足迹实体
     */
    public void insert(FootprintEntity footprint) {
        repository.insert(footprint);
    }

    /**
     * 更新足迹记录
     * @param footprint 足迹实体
     */
    public void update(FootprintEntity footprint) {
        repository.update(footprint);
    }

    /**
     * 删除足迹记录
     * @param footprint 足迹实体
     */
    public void delete(FootprintEntity footprint) {
        repository.delete(footprint);
    }

    /**
     * 删除所有足迹记录
     */
    public void deleteAllFootprints() {
        repository.deleteAllFootprints();
    }
    
    /**
     * 刷新足迹数据
     */
    public void refreshFootprints() {
        // 触发数据更新
        updateCityCount();
    }
}