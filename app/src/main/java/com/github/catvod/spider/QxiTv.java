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
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QxiTv extends Spider {

    private static final String siteUrl = "https://7xi.tv";
    private static final String cateUrl = siteUrl + "/index.php/api/vod";
    private static final String detailUrl = siteUrl + "/voddetail/";
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
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        int index = 1;
        for (Element element : doc.select("h4.title-h")) {
            String typeId = element.select("a").attr("href");
            String typeName = element.select("span").text();
            if (!"".equals(typeName) && index != 1 && index != 6) {
                classes.add(new Class(typeId, typeName.replace(" 查看更多","")));
            }
            index++;
        }
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
        String target = cateUrl + tid;
        HashMap<String, String> params = new HashMap<>();
        params.put("type", tid.split("/")[2].replace(".html", ""));
        params.put("page", pg);
        String time = String.valueOf(System.currentTimeMillis());
        params.put("time", time);
        String string = "DS" + time + "DCC147D11943AF75";
        params.put("key", MD5(string));
        String data = OkHttp.post(target, params);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        JsonArray jsonArray = jsonObject.getAsJsonArray("list");
        for (JsonElement element : jsonArray) {
            String id = String.valueOf(element.getAsJsonObject().get("vod_id"));
            String name = String.valueOf(element.getAsJsonObject().get("vod_name")).replace("\"", "");
            String pic = String.valueOf(element.getAsJsonObject().get("vod_pic")).replace("\"", "");
            if (!pic.startsWith("http")) {
                pic = siteUrl + pic;
            }
            list.add(new Vod(id, name, pic));
        }
        Integer total = (Integer.parseInt(pg) + 1) * 20;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)).concat(".html"), getHeaders()));
        String name = doc.select("div.this-desc-title").text();
        String pic = doc.select("div.this-pic-bj").attr("style").replace("background-image: url('", "").replace("')", "");
        String year = doc.select("div.this-desc-info > span").get(1).text();

        // 播放源
        Elements tabs = doc.select("a.swiper-slide");
        Elements list = doc.select("div.anthology-list-box.none");
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
                if (!"".equals(liUrl)){
                    liUrl = liUrl + "#" +  li.get(i1).text() + "$" + li.get(i1).attr("href");
                }else {
                    liUrl = liUrl + li.get(i1).text() + "$" + li.get(i1).attr("href");
                }
            }
            if (!"".equals(PlayUrl)) {
                PlayUrl = PlayUrl + "$$$" + liUrl;
            }else {
                PlayUrl = PlayUrl + liUrl;
            }
        }

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year);
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
        Document doc = Jsoup.parse(OkHttp.string(siteUrl.concat(id),getHeaders()));
        String regex = "\"url\":\"(.*?)m3u8\",";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(doc.html());
        String url = "";
        if (matcher.find()) {
            url = matcher.group(1);
            url = url.replace("\\/","/") + "m3u8";
        }
        return Result.get().url(url).header(getHeaders()).string();
    }
}
