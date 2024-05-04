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

public class ROU223 extends Spider {

    private static final String siteUrl = "http://223rou.com";
    private static final String searchUrl = siteUrl + "/index.php/vod/search.html?wd=";

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
        for (Element element : doc.select("div.menu.clearfix dl dd")) {
            if ( count >= 0 && count <= 16){
                String href = element.select("a").attr("href").replace("index.html","");
                String text = element.text();
                classes.add(new Class(href, text));
            }
            count ++;
        }
        doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("div.row.col5.clearfix dt a")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                String id = url;
                list.add(new Vod(id, name,siteUrl + pic));
            } catch (Exception e) {
            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = siteUrl + tid + "list_" + pg + ".html";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.row.col5.clearfix dt a")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                String id = url;
                list.add(new Vod(id, name, siteUrl + pic));
            } catch (Exception e) {
            }
        }

        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("title").text().split("-")[0];
        String regex = "playUrl=\"(.*?)m3u8\";";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(doc.html());
        String url = "";
        if (matcher.find()) {
            url = matcher.group(1);
            url = url.replace("\\/","/") + "m3u8";
        }

        String regex2 = "posterImg=\"(.*?)\";";

        Pattern pattern2 = Pattern.compile(regex2);
        Matcher matcher2 = pattern2.matcher(doc.html());
        String pic = "";
        if (matcher2.find()) {
            pic = matcher2.group(1);
            pic = pic.replace("\\/","/");
        }

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(name);
        vod.setVodPic(siteUrl + pic);
        vod.setVodPlayFrom("223ROU");
        vod.setVodPlayUrl("播放$" + url);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(key), getHeaders()));
        for (Element element : doc.select("div.row.col5.clearfix dt a")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                String id = url;
                list.add(new Vod(id, name, siteUrl + pic));
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
