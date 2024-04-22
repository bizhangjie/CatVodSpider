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

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NCat extends Spider {

    private static final String siteUrl = "https://www.ncat3.com:51111";
    private static final String picUrl = "https://vres.a357899.cn";
    private static final String cateUrl = siteUrl + "/show/";
    private static final String detailUrl = siteUrl + "/detail/";
    private static final String searchUrl = siteUrl + "/search?k=";
    private static final String playUrl = siteUrl + "/play/";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"1", "2", "3", "4", "6"};
        String[] typeNameList = {"电影", "连续剧", "动漫", "综艺", "短剧"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.module-item")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.select("a").attr("href");
                String name = element.select("img").attr("title");
                if (!pic.startsWith("http")) {
                    pic = picUrl + pic;
                }
                String id = url.split("/")[2];
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {

            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "-----3-" + pg + ".html";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.module-item")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.select("a").attr("href");
                String name = element.select("img").attr("title");
                if (!pic.startsWith("http")) {
                    pic = picUrl + pic;
                }
                String id = url.split("/")[2];
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {

            }
        }
        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("div.detail-title strong").text();
        String pic = doc.select(".detail-pic img").attr("data-original");
        String year = doc.select("a.detail-tags-item").get(0).text();
        String desc = doc.select("div.detail-desc p").text();

        // 播放源
        Elements tabs = doc.select("a.source-item span");
        Elements list = doc.select("div.episode-list");
        String PlayFrom = "";
        String PlayUrl = "";
        for (int i = 0; i < tabs.size(); i++) {
            String tabName = tabs.get(i).text();
            if (!"".equals(PlayFrom)) {
                PlayFrom = PlayFrom + "$$$" + tabName;
            } else {
                PlayFrom = PlayFrom + tabName;
            }
            Elements li = list.get(i).select("a");
            String liUrl = "";
            for (int i1 = 0; i1 < li.size(); i1++) {
                if (!"".equals(liUrl)) {
                    liUrl = liUrl + "#" + li.get(i1).text() + "$" + li.get(i1).attr("href").replace("/play/", "");
                } else {
                    liUrl = liUrl + li.get(i1).text() + "$" + li.get(i1).attr("href").replace("/play/", "");
                }
            }
            if (!"".equals(PlayUrl)) {
                PlayUrl = PlayUrl + "$$$" + liUrl;
            } else {
                PlayUrl = PlayUrl + liUrl;
            }
        }

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(picUrl + pic);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodContent(desc);
        vod.setVodPlayFrom(PlayFrom);
        vod.setVodPlayUrl(PlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)).concat(".html"), getHeaders()));
        for (Element element : doc.select("a.search-result-item")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.select("img").attr("title");
                if (!pic.startsWith("http")) {
                    pic = picUrl + pic;
                }
                String id = url.split("/")[2];
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {
            }
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(playUrl.concat(id), getHeaders()));
        String regex = "src: \"(.*?)m3u8\",";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(doc.html());
        String url = "";
        if (matcher.find()) {
            url = matcher.group(1);
            url = url.replace("\\/", "/") + "m3u8";
        }
        return Result.get().url(url).header(getHeaders()).string();
    }
}
