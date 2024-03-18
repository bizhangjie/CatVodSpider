package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RouVideo extends Spider {

    private static final String siteUrl = "https://rouva1.xyz";
    private static final String cateUrl = siteUrl + "/t/";
    private static final String detailUrl = siteUrl + "/v/";
    private static final String searchUrl = siteUrl + "/search?q=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"國產AV", "自拍流出", "探花", "OnlyFans", "日本"};
        String[] typeNameList = {"國產AV", "自拍流出", "探花", "OnlyFans", "日本"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl + "/v", getHeaders()));
        for (Element element : doc.select("div.relative")) {
            String pic = element.select("img").attr("src");
            String url = element.select("a").attr("href");
            String name = element.select("img").attr("alt");
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "?order=createdAt&page=" + pg;
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.relative")) {
            String pic = element.select("img").attr("src");
            String url = element.select("a").attr("href");
            String name = element.select("img").attr("alt");
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String desc = doc.select("meta[property=og:description]").attr("content");
        String year = doc.select("span.text-xs").get(0).text();
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodContent(desc);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodPlayFrom("Rou视频");
        // 获取播放地址
        String string = OkHttp.string(siteUrl + "/api/v/" + ids.get(0));
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(string, JsonObject.class);
        JsonObject video = jsonObject.getAsJsonObject("video");
        String playUrl = video.get("videoUrl").toString().replace("\"", "");
        vod.setVodPlayUrl("播放$" + playUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)), getHeaders()));
        for (Element element : doc.select("div.relative")) {
            String pic = element.select("img").attr("src");
            String url = element.select("a").attr("href");
            String name = element.select("img").attr("alt");
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
