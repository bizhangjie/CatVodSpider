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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiMei extends Spider {

    private static final String siteUrl = "https://infmbln.info";
    private static final String cateUrl = siteUrl;
    private static final String detailUrl = siteUrl;
    private static final String searchUrl = "https://api.3bmmjla.life/Api/getSearch";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    public List<Vod> parseHtml(Document document) {
        List<Vod> list = new ArrayList<>();
        for (Element element : document.select("div.pos")) {
            try {
                String pic = element.select("img").attr("src");
                String id = element.select("a").attr("href");
                String name = element.select("a").attr("title").replace("迷妹推荐--", "");
                if (!"".equals(name)) {
                    list.add(new Vod(id, name, pic));
                }
            } catch (Exception e) {
            }
        }
        return list;
    }

    public List<Vod> parseHtmlZB(Document document) {
        List<Vod> list = new ArrayList<>();
        for (Element element : document.select("#zhibo")) {
            try {
                String pic = element.select("img").attr("src");
                String id = element.select("a").attr("href");
                String name = element.select("a").attr("title");
                if (!"".equals(name)) {
                    list.add(new Vod(id, name, pic));
                }
            } catch (Exception e) {
            }
        }
        return list;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.hend").select("li")) {
            try {
                String typeId = element.select("a").attr("href");
                String typeName = element.select("a").text();
                classes.add(new Class(typeId, typeName));
            } catch (Exception e) {

            }
        }
        doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        List<Vod> list = parseHtml(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid;
        if ("1".equals(pg)){
            target = target ;
        }else {
            target = target + "/index_" + pg + ".html";
        }
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));

        List<Vod> list;
        if ("/suoyoushipin/zhibo".equals(tid)){
            list = parseHtmlZB(doc);
        }else {
            list = parseHtml(doc);
        }
        Integer total = (Integer.parseInt(pg) + 1) * 30;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 30, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0))));
        String name = doc.select("title").text().replace("迷妹推荐--", "");

        Pattern pattern = Pattern.compile("vHLSurl = \\\"(.*?)\\\";");
        Matcher matcher = pattern.matcher(doc.html());
        String PlayUrl = "";
        // 提取匹配到的内容
        if (matcher.find()) {
            PlayUrl = matcher.group(1);
        }
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(name.replace("迷妹网",""));
        vod.setVodPlayFrom("MiMei");
        vod.setVodPlayUrl("播放$" + "https://3bmmikh.life/new/hls" + PlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String target = searchUrl;
        Map<String, String> params = new HashMap<>();
        params.put("className", "ed5315ea37ade2181edbd8b27b3fc881");
        params.put("keyword", key);
        params.put("page", "1");
        params.put("limit", "24");
        String data = OkHttp.post(target, params);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        JsonArray array = jsonObject.getAsJsonArray("data");
        List<Vod> list = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String title = object.get("title").toString().replace("\"", "");
            String pic = "https://3bmmaeh.life/pic" + object.get("titlepic").toString().replace("\"", "");
            String id = object.get("titleurl").toString().replace("\"", "");
            list.add(new Vod(id, title, pic));
        }

        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
