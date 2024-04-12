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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ikanbot extends Spider {

    private static final String siteUrl = "https://v.ikanbot.com";
    private static final String cateUrl = siteUrl + "/hot";
    private static final String detailUrl = siteUrl + "/play/";
    private static final String searchUrl = siteUrl + "/search?q=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("a.item")) {
            String pic = element.select("img").attr("data-src");
            String url = element.attr("href");
            String name = element.select("img").attr("alt");
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        return list;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"/index-movie-热门", "/index-tv-热门", "/index-tv-国产剧", "/index-tv-韩剧"};
        String[] typeNameList = {"热门电影", "热门剧集", "国产剧", "韩剧"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl + "/billboard.html", getHeaders()));
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("div.item-root")) {
            String pic = element.select("img").attr("data-src");
            String url = element.select("a").attr("href");
            String name = element.select("img").attr("alt");
            try {
                String id = url.split("/")[2];
                list.add(new Vod(id, name, pic));
            } catch (Exception e) {

            }
        }
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid;
        if (!"1".equals(pg)){
            target = target + "-p-" + pg;
        }
        Document doc = Jsoup.parse(OkHttp.string(target.concat(".html"), getHeaders()));
        List<Vod> list = parseVods(doc);
        Integer total = (Integer.parseInt(pg) + 1) * 24;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 24, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders()));
        String name = doc.select("h1").text();
        String pic = doc.select("meta[property=og:image]").attr("content");
        Elements desc = doc.select("div.detail > h3");
        String year = desc.get(1).text();
        String area = desc.get(2).text();
        String actor = desc.get(3).text();

        String current_id = doc.select("input#current_id").attr("value");
        String e_token = doc.select("input#e_token").attr("value");
        String mtype = doc.select("input#mtype").attr("value");
        String tks = get_tks(current_id, e_token);
        String url = siteUrl + "/api/getResN?videoId=" + ids.get(0) + "&mtype=" + mtype + " &token=" + tks;
        String data = OkHttp.string(url, getHeaders());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        JsonArray array = jsonObject.getAsJsonObject("data").getAsJsonArray("list");
        String PlayFrom = "";
        String PlayUrl = "";
        for (JsonElement element : array) {

            // 使用正则表达式匹配 "flag" 和 "url"
            Pattern pattern = Pattern.compile("\\\"flag\\\":\\\"(.*?)\\\",\\\"url\\\":\\\"(.*?)\\\"");
            Matcher matcher = pattern.matcher(String.valueOf(element.getAsJsonObject().get("resData")).replace("\\",""));
            String flag = "";
            String liUrl = "";
            // 提取匹配到的内容
            if (matcher.find()) {
                flag = matcher.group(1);
                liUrl = matcher.group(2);
            }
            if (!"".equals(PlayFrom)) {
                PlayFrom = PlayFrom + "$$$" + flag;
            } else {
                PlayFrom = PlayFrom + flag;
            }
            if (!"".equals(PlayUrl)) {
                PlayUrl = PlayUrl + "$$$" + liUrl.replace("$" + flag,"");
            } else {
                PlayUrl = PlayUrl + liUrl.replace("$" + flag,"");
            }

//            PlayUrl += String.valueOf(element.getAsJsonObject().get("resData")).replace("\\","").replace("\\\"","\"");
        }
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodActor(actor);
        vod.setVodArea(area);
        vod.setVodName(name);
        vod.setVodPlayFrom(PlayFrom);
        vod.setVodPlayUrl(PlayUrl.replace("##","#").replace("#$$$","$$$"));
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(searchUrl.concat(URLEncoder.encode(key)), getHeaders()));
        List<Vod> list = new ArrayList<>();
        for (Element element : doc.select("a.cover-link")) {
            String pic = element.select("img").attr("data-src");
            String url = element.attr("href");
            String name = element.select("img").attr("alt");
            String id = url.split("/")[2];
            list.add(new Vod(id, name, pic));
        }
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws
            Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }


//    function get_tks() {
//                const _0xf07220 = _0xf746;
//        let _0x35162d = document['getElementById'] ('current_id').value 'current_id'
//                , _0xf25678 = document['getElementById'] ('e_token').value;
//        'e_token'
//        if (!_0x35162d || !_0xf25678)
//            return;
//        let _0x3882a3 = _0x35162d['length'], _0x52a097 = _0x35162d['substring']
//        (_0x3882a3 - 4, _0x3882a3)
//                  ,_0x2d9d1b = [];
//        for (let _0x570711 = 0x0; _0x570711 < _0x52a097['length']; _0x570711++) {
//            let _0x23e537 = parseInt(_0x52a097[_0x570711]), _0x48b93d = _0x23e537 % 0x3 + 0x1;
//            _0x2d9d1b[_0x570711] = _0xf25678['substring'] (_0x48b93d, _0x48b93d + 0x8),
//            _0xf25678 = _0xf25678['substring'] (_0x48b93d + 0x8, _0xf25678['length']);
//        }
//        v_tks = _0x2d9d1b['join'] ('');
//    }

    public String get_tks(String current_id, String e_token) {
//        String current_id = "798347";
//        String e_token = "mre0530ce88964487488y67d38c0a1uj7fd15cb8";
        System.out.printf("current_id " + current_id);
        System.out.printf("e_token " + e_token);
        if ("".equals(current_id) || "".equals(e_token)) {
            return "";
        }
        String[] list = new String[4];
        int idLength = current_id.length();
        String subString = current_id.substring(idLength - 4, idLength);
        for (int i = 0; i < subString.length(); i++) {
            int num = Character.getNumericValue(subString.charAt(i));
            int begin = num % 3 + 1;
            list[i] = e_token.substring(begin, begin + 8);
            e_token = e_token.substring(begin + 8);
        }

        StringBuilder v_tks = new StringBuilder();
        for (String string : list) {
            v_tks.append(string);
        }
        return v_tks.toString();
    }

}