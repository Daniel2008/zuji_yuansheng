package com.damors.zuji.utils;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.UUID;

public class DeviceUtils {

    /**
     * 获取设备唯一标识符
     *
     * @param context 上下文
     * @return 设备ID
     */
    public static String getDeviceId(Context context) {
        String deviceId;

        try {
            // 首先尝试获取Android ID
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            // 如果Android ID为空或者是已知的错误值，则生成一个UUID
            if (deviceId == null || deviceId.isEmpty() || deviceId.equals("9774d56d682e549c")) {
                // 生成一个UUID并保存到SharedPreferences
                final String PREFS_FILE = "device_id_prefs";
                final String DEVICE_ID_KEY = "device_id";

                deviceId = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                        .getString(DEVICE_ID_KEY, null);

                if (deviceId == null) {
                    deviceId = UUID.randomUUID().toString();
                    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                            .edit()
                            .putString(DEVICE_ID_KEY, deviceId)
                            .apply();
                }
            }
        } catch (Exception e) {
            // 如果出现任何异常，生成一个随机UUID
            deviceId = UUID.randomUUID().toString();
        }

        return deviceId;
    }
}
