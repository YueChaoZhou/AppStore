package com.bb.googleplaybb.net.protocol;

import com.bb.googleplaybb.domain.AppInfo;
import com.bb.googleplaybb.domain.TopNew;
import com.bb.googleplaybb.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Boby on 2018/7/13.
 */

public class HomeNetProtocol extends BaseNetProtocol<ArrayList<AppInfo>> {

    public ArrayList<TopNew> getPictures() {
        return pictures;
    }

    private ArrayList<TopNew> pictures;

    @Override
    public ArrayList<AppInfo> parseData(String json) {
        try {
            if (!StringUtils.isEmpty(json)) {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray appInfoArray = jsonObject.getJSONArray("list");

                if (jsonObject.has("picture")) {
                    JSONArray ja = jsonObject.getJSONArray("picture");
                    pictures = new ArrayList<>();
                    for (int j = 0; j < ja.length(); j++) {
                        JSONObject jo = ja.getJSONObject(j);
                        TopNew topNew = new TopNew();
                        topNew.setName(jo.getString("name"));
                        topNew.setPackageName(jo.getString("packageName"));
                        topNew.setUrl(jo.getString("url"));
                        pictures.add(topNew);
                    }
                }

                if (appInfoArray != null) {
                    ArrayList<AppInfo> appInfos = new ArrayList<>();
                    for (int i = 0; i < appInfoArray.length(); i++) {
                        AppInfo appInfo = new AppInfo();
                        JSONObject appInfoObject = appInfoArray.getJSONObject(i);
                        appInfo.id = appInfoObject.getString("id");
                        appInfo.name = appInfoObject.getString("name");
                        appInfo.packageName = appInfoObject.getString("packageName");
                        appInfo.iconUrl = appInfoObject.getString("iconUrl");
                        appInfo.stars = (float) appInfoObject.getDouble("stars");
                        appInfo.size = appInfoObject.getLong("size");
                        appInfo.downloadUrl = appInfoObject.getString("downloadUrl");
                        appInfo.des = appInfoObject.getString("des");

                        appInfos.add(appInfo);//添加到集合中
                    }
                    return appInfos;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getParams() {
        return "";
    }

    @Override
    public String getKey() {
        return "app/homelist";
    }

    public String getCacheName() {
        return "homelist";
    }
}
