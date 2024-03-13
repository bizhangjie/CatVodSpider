package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XVideos extends Spider {

    private static final String siteUrl = "https://cn.xvideos2.uk";
    private static final String cateUrl = siteUrl + "/best/";
    private static final String detailUrl = siteUrl;
    private static final String searchUrl = siteUrl + "/?k=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(cateUrl,getHeaders()));
        for (Element element : doc.select("div#date-links-pagination").select("li")) {
            try {
                String typeId = element.select("a").attr("href").split("/")[2];
                String typeName = element.select("a").text();
                classes.add(new Class(typeId, typeName));
            }catch (Exception e){

            }
        }
        doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.thumb-block")) {
            try {
                String pic = element.select("img").attr("src");
                String id = element.select("a").attr("href");
                String name = element.select("p.title").select("a").attr("title");
                list.add(new Vod(id, name, pic));
            }catch (Exception e){

            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "/" + pg;
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.thumb-block")) {
            try {
                String pic = element.select("img").attr("src");
                String id = element.select("a").attr("href");
                String name = element.select("p.title").select("a").attr("title");
                list.add(new Vod(id, name, pic));
            }catch (Exception e){

            }
        }
        Integer total = (Integer.parseInt(pg)+1)*20;
        return Result.string(Integer.parseInt(pg),Integer.parseInt(pg)+1,20,total,list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat("/"), getHeaders()));
        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");

        Pattern pattern = Pattern.compile("\"contentUrl\\\": \\\"(.*?)\\\",");
        Matcher matcher = pattern.matcher(doc.html());
        String PlayUrl = "";
        // 提取匹配到的内容
        if (matcher.find()) {
            PlayUrl = matcher.group(1);
        }
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("XVideos");
        vod.setVodPlayUrl("播放$" + PlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = searchUrl + URLEncoder.encode(key) + "&p=1";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.thumb-block")) {
            try {
                String pic = element.select("img").attr("src");
                String id = element.select("a").attr("href");
                String name = element.select("p.title").select("a").attr("title");
                list.add(new Vod(id, name, pic));
            }catch (Exception e){

            }
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
