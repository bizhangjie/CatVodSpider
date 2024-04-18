package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YingTan extends Spider {

    private static final String siteUrl = "http://cms-vip.lyyytv.cn";
    private static final String cateUrl = siteUrl + "/api.php/app/video?tid=";
    private static final String detailUrl = siteUrl + "/api.php/app/video_detail?id=";
    private static final String playUrl = siteUrl + "/api.php/app/";
    private static final String searchUrl = siteUrl + "/api.php/app/search?text=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Dart/2.14 (dart:io)");
        headers.put("Host", siteUrl.replace("http://", ""));
        headers.put("Connection", "Keep-Alive");
//        headers.put("Accept-Encoding", "gzip");
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        String data = OkHttp.string(siteUrl.concat("/api.php/app/nav"), getHeaders());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        JsonArray array = jsonObject.getAsJsonArray("list");
        for (JsonElement jsonElement : array) {
            JsonObject element = jsonElement.getAsJsonObject();
            String typeId = element.get("type_id").getAsString();
            String typeName = element.get("type_name").getAsString();
            classes.add(new Class(typeId, typeName));
        }

        String dataIndex = OkHttp.string(siteUrl.concat("/api.php/app/index_video?token="), getHeaders());
        JsonObject jsonObject1 = new Gson().fromJson(dataIndex, JsonObject.class);
        JsonArray array1 = jsonObject1.getAsJsonArray("list");
        for (JsonElement jsonElement : array1) {
            JsonObject element = jsonElement.getAsJsonObject();
            JsonArray vlist = element.getAsJsonArray("vlist");
            for (JsonElement jsonElement1 : vlist) {
                JsonObject element2 = jsonElement1.getAsJsonObject();
                String vodId = element2.get("vod_id").getAsString();
                String vodName = element2.get("vod_name").getAsString();
                String vodPic = element2.get("vod_pic").getAsString();
                list.add(new Vod(vodId, vodName, vodPic));
            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "&class=&area=&lang=&year=&limit=18&pg=" + pg;
        String dataIndex = OkHttp.string(target, getHeaders());
        JsonObject jsonObject1 = new Gson().fromJson(dataIndex, JsonObject.class);
        JsonArray array1 = jsonObject1.getAsJsonArray("list");
        for (JsonElement jsonElement : array1) {
            JsonObject element2 = jsonElement.getAsJsonObject();
            String vodId = element2.get("vod_id").getAsString();
            String vodName = element2.get("vod_name").getAsString();
            String vodPic = element2.get("vod_pic").getAsString();
            list.add(new Vod(vodId, vodName, vodPic));
        }

        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String data = OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders());
        JsonObject jsonObject = new Gson().fromJson(data, JsonObject.class);
        JsonObject asJsonObject = jsonObject.getAsJsonObject("data");
        String vodName = asJsonObject.get("vod_name").getAsString();
        String vodPic = asJsonObject.get("vod_pic").getAsString();
        String vodActor = asJsonObject.get("vod_actor").getAsString();
        String vodRemarks = asJsonObject.get("vod_remarks").getAsString();
        String vodYear = asJsonObject.get("vod_year").getAsString();
        String vodContent = asJsonObject.get("vod_content").getAsString();
        String vodPlayFrom = asJsonObject.get("vod_play_from").getAsString();
        String vodPlayUrl = asJsonObject.get("vod_play_url").getAsString();

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(vodPic);
        vod.setVodName(vodName);
        vod.setVodActor(vodActor);
        vod.setVodYear(vodYear);
        vod.setVodRemarks(vodRemarks);
        vod.setVodContent(vodContent);
        vod.setVodPlayFrom(vodPlayFrom);
        vod.setVodPlayUrl(vodPlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        String dataIndex = OkHttp.string(searchUrl.concat(key), getHeaders());
        JsonObject jsonObject1 = new Gson().fromJson(dataIndex, JsonObject.class);
        JsonArray array1 = jsonObject1.getAsJsonArray("list");
        for (JsonElement jsonElement : array1) {
            JsonObject element2 = jsonElement.getAsJsonObject();
            String vodId = element2.get("vod_id").getAsString();
            String vodName = element2.get("vod_name").getAsString();
            String vodPic = element2.get("vod_pic").getAsString();
            list.add(new Vod(vodId, vodName, vodPic));
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).string();
    }
}
