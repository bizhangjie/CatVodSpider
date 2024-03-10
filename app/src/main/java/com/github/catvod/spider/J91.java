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

public class J91 extends Spider {

    private static final String siteUrl = "https://pta.9a07g.com";
    private static final String cateUrl = siteUrl + "/video/category/";
    private static final String detailUrl = siteUrl + "/video/view/";
    private static final String searchUrl = siteUrl + "/search?keywords=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("div.video-elem")) {
            String pic = element.select("div.img").attr("style");
            String url = element.select("a").attr("href");
            String name = element.select("a.title").text();
            if (pic.endsWith(".gif") || name.isEmpty()) continue;
            if (!url.startsWith("http")) {
                pic = pic.replace("background-image: url('", "").replace("')", "");
                if (!pic.startsWith("http")) pic = "https:" + pic;
                String id = url.split("/")[3];
                list.add(new Vod(id, name, pic));
            }
        }
        return list;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"latest","hd","recent-favorite","hot-list","recent-rating","nonpaid","ori","long-list","longer-list","month-discuss","top-favorite","most-favorite","top-list","top-last"};
        String[] typeNameList = {"最近更新","高清视频","最近加精","当前最热","最近得分","非付费","91原创","10分钟以上","20分钟以上","本月讨论","本月收藏","收藏最多","本月最热","上月最热"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid + "/" + pg;
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        Integer total = (Integer.parseInt(pg)+1) * 20;
        return Result.string(Integer.parseInt(pg),Integer.parseInt(pg)+1,20,total,list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String year = doc.select("meta[property=video:release_date]").attr("content");
        String playUrl = doc.select("#video-play").attr("data-src");
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodPlayFrom("J91");
        vod.setVodPlayUrl("播放$" + playUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)), getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
