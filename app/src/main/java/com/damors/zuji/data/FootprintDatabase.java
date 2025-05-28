package com.damors.zuji.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * 足迹数据库类
 * 使用Room数据库框架
 */
@Database(entities = {FootprintEntity.class}, version = 2, exportSchema = false)
public abstract class FootprintDatabase extends RoomDatabase {

    // 数据库实例
    private static FootprintDatabase instance;

    // 获取DAO接口
    public abstract FootprintDao footprintDao();

    /**
     * 获取数据库实例，使用单例模式
     * @param context 应用上下文
     * @return 数据库实例
     */
    public static synchronized FootprintDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    FootprintDatabase.class, "footprint_database")
                    .fallbackToDestructiveMigration() // 当版本升级时，重建数据库
                    .build();
        }
        return instance;
    }
}