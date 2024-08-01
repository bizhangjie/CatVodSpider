package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;
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
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMVideo extends Spider {

    private static final String siteUrl = "https://www.qq99pp.com";

    private static final String Init = "https://spiderscloudcn2.51111666.com/getDataInit";

    private static final String cateUrl = "https://spiderscloudcn2.51111666.com/forward";
    private static final String detailUrl = siteUrl + "/play/";
    private static final String playUrl = siteUrl + "/play/";
    private static final String searchUrl = siteUrl + "/search--------------.html?wd=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        String target = Init;
        // 构建JSON格式的请求体，包含整数
        String json = "{\"name\":\"John\", \"age\":31, \"city\":\"New York\"}";
        String data = OkHttp.post(target, json);
        // json处理
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        JsonObject asJsonObject = jsonObject.getAsJsonObject("data");
        JsonArray menu0ListMap = asJsonObject.getAsJsonArray("menu0ListMap");
        // 输出menu0ListMap的前三个元素
        for (int i = 0; i < menu0ListMap.size() && i < 3; i++) {
            JsonObject object = menu0ListMap.get(i).getAsJsonObject();
            JsonArray menu2List = object.getAsJsonArray("menu2List");
            for (int j = 0; j < menu2List.size(); j++) {
                JsonObject object1 = menu2List.get(j).getAsJsonObject();
                String typeName2 = object1.get("typeName2").getAsString();
                String typeId2 = object1.get("typeId2").getAsString().replace(".0", "");
                classes.add(new Class(typeId2, typeName2));
            }
        }

//        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
//        for (Element element : doc.select("a.vodlist_thumb")) {
//            try {
//                String pic = element.attr("data-original");
//                String url = element.attr("href");
//                String name = element.attr("title");
//                if (!pic.startsWith("http")) {
//                    pic = siteUrl + pic;
//                }
//                String id = url.split("/")[2];
//                list.add(new Vod(id, name, pic));
//            } catch (Exception e) {
//            }
//        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {

        // 构建JSON格式的请求体，包含整数
        String json = "{\"command\":\"WEB_GET_INFO\",\"pageNumber\":" + pg + ",\"RecordsPage\":20,\"typeId\":\"" + tid
                + "\",\"typeMid\":\"1\",\"languageType\":\"CN\",\"content\":\"\"}";

        List<Vod> list = new ArrayList<>();
        String target = cateUrl;
        String data = OkHttp.post(target, json);

        // json处理
        Gson gson = new Gson();
        JsonObject fromJson = gson.fromJson(data, JsonObject.class);
        JsonObject data1 = fromJson.getAsJsonObject("data");
        JsonArray menu0ListMap = data1.getAsJsonArray("resultList");
        // 输出menu0ListMap的前三个元素
        for (int i = 0; i < menu0ListMap.size(); i++) {
            JsonObject object = menu0ListMap.get(i).getAsJsonObject();
            String vod_name = object.get("vod_name").getAsString().replace("yy8ycom", "");
            String vod_pic = object.get("vod_pic").getAsString();
            String id = vod_name + "#" + vod_pic;
            list.add(new Vod(id, vod_name, vod_pic));
        }

        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {

        String vod_name = ids.get(0).split("#")[0];
        String vod_pic = ids.get(0).split("#")[1];
        String PlayUrl = vod_pic.replace("1.jpg","playlist.m3u8");
        Vod vod = new Vod();
        vod.setVodId(vod_name);
        vod.setVodPic(vod_pic);
        vod.setVodName(vod_name);
        vod.setVodPlayFrom("熊猫视频");
        vod.setVodPlayUrl("播放$" + PlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {

        String json = "{\"command\":\"WEB_GET_INFO\",\"pageNumber\":1,\"RecordsPage\":20,\"typeId\":\"0\",\"typeMid\":\"1\",\"languageType\":\"CN\",\"content\":\""+
                key + "\",\"type\":\"1\"}";

        List<Vod> list = new ArrayList<>();
        String target = cateUrl;
        String data = OkHttp.post(target, json);

        // json处理
        Gson gson = new Gson();
        JsonObject fromJson = gson.fromJson(data, JsonObject.class);
        JsonObject data1 = fromJson.getAsJsonObject("data");
        JsonArray menu0ListMap = data1.getAsJsonArray("resultList");
        // 输出menu0ListMap的前三个元素
        for (int i = 0; i < menu0ListMap.size(); i++) {
            JsonObject object = menu0ListMap.get(i).getAsJsonObject();
            String vod_name = object.get("vod_name").getAsString().replace("yy8ycom", "");
            String vod_pic = object.get("vod_pic").getAsString();
            String id = vod_name + "#" + vod_pic;
            list.add(new Vod(id, vod_name, vod_pic));
        }

        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String url = id;
        return Result.get().url(url).header(getHeaders()).string();
    }
}
