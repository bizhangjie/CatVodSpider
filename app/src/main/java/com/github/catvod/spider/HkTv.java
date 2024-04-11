package com.github.catvod.spider;

import android.os.Build;
import android.util.Base64;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HkTv extends Spider {

    private static final String siteUrl = "http://www.tvyb04.com";
    private static final String cateUrl = siteUrl + "/vod/type/id/";
    private static final String detailUrl = siteUrl + "/vod/detail/id/";
    private static final String playUrl = siteUrl + "/vod/play/id/";
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
        String[] typeIdList = {"1", "2", "3", "4", "19"};
        String[] typeNameList = {"电影", "电视剧", "综艺", "动漫", "短片"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("a.myui-vodlist__thumb")) {
            try {
                String pic = element.attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                if (!pic.startsWith("http")) {
                    pic = siteUrl + pic;
                }
                String id = url.split("/")[4];
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {
            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + ".html";
        if (!"1".equals(pg)) {
            target = pg + "/page/" + tid + ".html";
        }
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("ul.myui-vodlist li a.myui-vodlist__thumb")) {
            try {
                String pic = element.attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                if (!pic.startsWith("http")) {
                    pic = siteUrl + pic;
                }
                String id = url.split("/")[4];
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
        String name = doc.select("h1.title").text();
        String pic = doc.select("a.myui-vodlist__thumb.picture img").attr("data-original");
        // 播放源
        Elements tabs = doc.select("div.myui-panel__head.bottom-line.active.clearfix h3");
        Elements list = doc.select("ul.myui-content__list");
        String PlayFrom = "";
        String PlayUrl = "";
        for (int i = 1; i < tabs.size() - 1; i++) {
            String tabName = tabs.get(i).text();
            if (!"".equals(PlayFrom)) {
                PlayFrom = PlayFrom + "$$$" + tabName;
            } else {
                PlayFrom = PlayFrom + tabName;
            }
            Elements li = list.get(i - 1).select("a");
            String liUrl = "";
            for (int i1 = 0; i1 < li.size(); i1++) {
                if (!"".equals(liUrl)) {
                    liUrl = liUrl + "#" + li.get(i1).text() + "$" + li.get(i1).attr("href").replace("/vod/play/id/", "");
                } else {
                    liUrl = liUrl + li.get(i1).text() + "$" + li.get(i1).attr("href").replace("/vod/play/id/", "");
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
        vod.setVodPic(siteUrl + pic);
        vod.setVodName(name);
        vod.setVodPlayFrom(PlayFrom);
        vod.setVodPlayUrl(PlayUrl);
        return Result.string(vod);
    }

    @Override
    // 需要验证码
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)), getHeaders()));
        for (Element element : doc.select("div.searchlist_img")) {
            try {
                String pic = element.select("a").attr("data-original");
                String url = element.select("a").attr("href");
                String name = element.select("a").attr("title");
                if (!pic.startsWith("http")) {
                    pic = siteUrl + pic;
                }
                String id = url.replace("/video/", "").replace(".html", "-1-1.html");
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {
            }
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String target = playUrl.concat(id);
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        String regex = "\"url\\\":\\\"(.*?)\\\",\\\"url_next\\\":";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(doc.html());
        String url = doc.html();
        if (matcher.find()) {
            String encryptedData = matcher.group(1);
            String decodedString = new String(Base64.decode(encryptedData, Base64.DEFAULT));
            url = decodeURL(decodedString);
        }
        return Result.get().url(url).header(getHeaders()).string();
    }

    public static String decodeURL(String encodedURL) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < encodedURL.length()) {
            if (encodedURL.charAt(index) == '%') {
                if (index + 2 < encodedURL.length()) {
                    if (encodedURL.charAt(index + 1) == 'u') {
                        String unicodeStr = encodedURL.substring(index + 2, index + 6);
                        char unicodeChar = (char) Integer.parseInt(unicodeStr, 16);
                        sb.append(unicodeChar);
                        index += 6;
                    } else {
                        String hexStr = encodedURL.substring(index + 1, index + 3);
                        char hexChar = (char) Integer.parseInt(hexStr, 16);
                        sb.append(hexChar);
                        index += 3;
                    }
                } else {
                    sb.append(encodedURL.charAt(index));
                    index++;
                }
            } else {
                sb.append(encodedURL.charAt(index));
                index++;
            }
        }
        return sb.toString();
    }
}
