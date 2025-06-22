package com.damors.zuji.manager;

import com.damors.zuji.model.AppUpdateInfo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuexiang.xupdate.entity.UpdateEntity;
import com.xuexiang.xupdate.listener.IUpdateParseCallback;
import com.xuexiang.xupdate.proxy.IUpdateParser;

import org.json.JSONObject;

class CustomUpdateParser implements IUpdateParser {
    @Override
    public UpdateEntity parseJson(String json) throws Exception {
        JsonElement jsonElement = JsonParser.parseString(json);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        if(jsonObject.get("code").getAsInt() == 200){
            AppUpdateInfo result = new Gson().fromJson(jsonObject.getAsJsonObject("data"), AppUpdateInfo.class);
            if (result != null) {
                UpdateEntity updateEntity = new UpdateEntity();
                if(AppUpdateManager.getCurrentVersionCode() < result.getVersionCode()){
                    updateEntity.setHasUpdate(true);
                }else {
                    updateEntity.setHasUpdate(false);
                }
                updateEntity.setIsIgnorable(true);
                updateEntity.setVersionCode(result.getVersionCode());
                updateEntity.setVersionName(result.getVersionName());
                updateEntity.setUpdateContent(result.getUpdateContent());
                updateEntity.setDownloadUrl(result.getDownloadUrl());
                updateEntity.setSize(result.getFileSize());
                return updateEntity;

            }
        }

        return null;
    }

    @Override
    public void parseJson(String json, IUpdateParseCallback callback) throws Exception {

    }

    @Override
    public boolean isAsyncParser() {
        return false;
    }
}