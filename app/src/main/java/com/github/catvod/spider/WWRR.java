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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Base64;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

public class WWRR extends Spider {

    private static final String siteUrl = "https://hd.6nu2.com";
    private static final String cateUrl = siteUrl + "/home/";
    private static final String detailUrl = siteUrl + "/play/";
    private static final String searchUrl = siteUrl + "/search/video/";

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
        String[] typeIdList = {"/"};
        String[] typeNameList = {"全部"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        for (Element element : doc.select("div.listA a")) {
            String pic = element.select("img").attr("src");
            String url = element.attr("href");
            String name = element.select("div.name").text();
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(classes, list);

    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String target = cateUrl + tid + "/" + pg + ".html";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("div.listA a")) {
            String pic = element.select("img").attr("src");
            String url = element.attr("href");
            String name = element.select("div.name").text();
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        Integer total = (Integer.parseInt(pg)+1)*20;
        return Result.string(Integer.parseInt(pg),Integer.parseInt(pg)+1,20,total,list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("div.name.WF").text().replace("2048.cc-","");
        String pic = doc.select("div.vjs-poster img").attr("src");

        Pattern pattern = Pattern.compile("vodurl = '(.*?)';");
        Matcher matcher = pattern.matcher(doc.html());
        String PlayUrl = "";
        // 提取匹配到的内容
        if (matcher.find()) {
            PlayUrl = matcher.group(1);
        }
        String decompressedUrl = "";
        try {
            String decompressedData = decompress(PlayUrl);
            JsonObject jsonObject = new Gson().fromJson(decompressedData, JsonObject.class);
            decompressedUrl = jsonObject.get("url").getAsString();
            System.out.println("解压缩后的数据: " + decompressedUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodName(name);
        vod.setVodPlayFrom("我为人人");
        vod.setVodPlayUrl("播放$" + decompressedUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(key).concat("/1.html"), getHeaders()));
        for (Element element : doc.select("div.listA a")) {
            String pic = element.select("img").attr("src");
            String url = element.attr("href");
            String name = element.select("div.name").text();
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }


    public static String decompress(String base64String) throws Exception {
        // 将Base64字符串转换为字节数组
        byte[] compressedData = Base64.decode(base64String, Base64.DEFAULT);

        // 使用Inflater解压缩数据
        Inflater inflater = new Inflater(true); // true表示处理raw deflate数据
        inflater.setInput(compressedData);

        byte[] buffer = new byte[1024];
        StringBuilder outputStream = new StringBuilder();
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.append(new String(buffer, 0, count));
        }
        inflater.end();

        return outputStream.toString();
    }
}