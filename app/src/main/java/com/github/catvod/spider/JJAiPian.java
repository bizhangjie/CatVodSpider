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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JJAiPian extends Spider {

    private static final String siteUrl = "https://www.15h2.com";
    private static final String cateUrl = siteUrl + "/type/";
    private static final String detailUrl = siteUrl + "/index.php/vod/play/id/";
    private static final String playUrl = siteUrl + "/playl/";
    private static final String searchUrl = siteUrl + "/vod/search.html?wd=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        Integer count = 0;
        for (Element element : doc.select("div.header-links.header-item a")) {
            if ( count >= 0 && count <= 16){
                String href = element.attr("href").replace("/type/","").replace(".html","");
                String text = element.text();
                classes.add(new Class(href, text));
            }
            count ++;
        }
        doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.featured-content-image a")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.select("img").attr("alt");
                String id = url.replace("/detail/","").replace(".html","");
                list.add(new Vod(id, name,pic));
            } catch (Exception e) {
            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "-" + pg + ".html";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.featured-content-image a")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.select("img").attr("alt");
                String id = url.replace("/detail/","").replace(".html","");
                list.add(new Vod(id, name,pic));
            } catch (Exception e) {
            }
        }

        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(playUrl.concat(ids.get(0)).concat("-1-1.html"), getHeaders()));
        String name = doc.select("li.active").text();
        String regex = "url:\"(.*?)m3u8\",";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(doc.html());
        String url = "";
        if (matcher.find()) {
            url = matcher.group(1);
            url = url.replace("\\/","/") + "m3u8";
        }
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(name);
        vod.setVodPlayFrom("99aipian");
        vod.setVodPlayUrl("播放$" + url);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(key), getHeaders()));
        for (Element element : doc.select("div.featured-content-image a")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.select("img").attr("alt");
                String id = url.replace("/detail/","").replace(".html","");
                list.add(new Vod(id, name,pic));
            } catch (Exception e) {
            }
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
