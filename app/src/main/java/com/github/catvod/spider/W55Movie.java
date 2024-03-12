package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.AESEncryption;
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

public class W55Movie extends Spider {

    private static final String siteUrl = "https://5view.shop";
    private static final String cateUrl = siteUrl + "/index.php/api/vod";
    private static final String detailUrl = siteUrl + "/voddetail/";
    private static final String playUrl = siteUrl + "/vodplay/";
    private static final String searchUrl = siteUrl + "/vodsearch/page/1/wd/";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"/label/netflix/page/","/vodshow/1","/vodshow/2","/vodshow/124","/vodshow/4","/vodshow/3"};
        String[] typeNameList = {"Netflix","电影","连续剧","福利","动漫","综艺"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        for (Element element : doc.select("a.module-poster-item")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                if (!pic.startsWith("http")) {
                    pic = siteUrl + pic;
                }
                String id = url.split("/")[2];
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {
            }
        }
        return Result.string(classes, list);
    }

    public String MD5(String string) {
        // 创建 MD5 实例
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算 MD5 哈希值
            byte[] hashBytes = md.digest(string.getBytes());

            // 将字节数组转换为十六进制字符串表示
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 输出加密后的 MD5 字符串
            System.out.println("MD5 加密: " + hexString.toString());
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        if (tid.startsWith("/label/")){
            tid = tid + pg + ".html";
        }else {
            tid = tid + "--------" + pg + "---.html";
        }
        String target = siteUrl + tid;
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        for (Element element : doc.select("a.module-poster-item")) {
            try {
                String pic = element.select("img").attr("data-original");
                String url = element.attr("href");
                String name = element.attr("title");
                if (!pic.startsWith("http")) {
                    pic = siteUrl + pic;
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
        String name = doc.select("h1").text();
        String pic = doc.select("img.ls-is-cached").attr("data-original");
        Elements desc = doc.select("div.module-info-tag-link");
        String year = desc.get(0).select("a").text();
        String area = desc.get(1).select("a").text();
        String tags = desc.get(2).select("a").text();
        String content = doc.select("meta[name=description]").attr("content");

        // 播放源
        Elements tabs = doc.select("div.module-tab-item");
        Elements list = doc.select("div.module-play-list-content");
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
                    liUrl = liUrl + "#" + li.get(i1).text() + "$" + li.get(i1).attr("href").replace("/vodplay/","");
                } else {
                    liUrl = liUrl + li.get(i1).text() + "$" + li.get(i1).attr("href").replace("/vodplay/","");
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
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodArea(area);
        vod.setVodTag(tags);
        vod.setVodContent(content);
        vod.setVodName(name);
        vod.setVodPlayFrom(PlayFrom);
        vod.setVodPlayUrl(PlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        List<Vod> list = new ArrayList<>();
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)).concat(".html"), getHeaders()));
        for (Element element : doc.select("a.public-list-exp")) {
            try {
                String pic = element.select("img").attr("data-src");
                String url = element.attr("href");
                String name = element.attr("title");
                if (!pic.startsWith("http")) {
                    pic = siteUrl + pic;
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
        String target = playUrl.concat(id);
        Document doc = Jsoup.parse(OkHttp.string(target));
        String regex = "\"url\\\":\\\"(.*?)\\\",\\\"url_next\\\":\\\"(.*?)\\\"";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(doc.html());
        String url = "";
        String url_next = "";
        if (matcher.find()) {
            url = matcher.group(1);
            url_next = matcher.group(2);
        }
        String encrytStr = "{\"url\":\"" + url + "\",\"next_url\":\"" + url_next + "\"}";
        // 加密
        String encrypt = AESEncryption.encrypt(encrytStr);
        String encodeURI = AESEncryption.encodeURIComponent(encrypt);
        // 请求获取url
        String data = OkHttp.string("https://player.ddzyku.com:3653/getUrls?data=" + encodeURI);
        // 解密
        String decrypted = AESEncryption.decrypt(data);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(decrypted, JsonObject.class);
        JsonObject dataObject = jsonObject.getAsJsonObject("data");
        String url1 = "";
        // Ensure the field exists before accessing it
        if (dataObject != null && dataObject.has("url")) {
            url1 = dataObject.get("url").getAsString();
        } else {
            System.out.println("Invalid JSON format or missing 'url' field.");
        }
        return Result.get().url(url1).header(getHeaders()).string();
    }
}
