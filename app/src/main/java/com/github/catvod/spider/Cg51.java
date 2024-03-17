package com.github.catvod.spider;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.CgImageUtil;
import com.github.catvod.utils.Util;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cg51 extends Spider {

    private static final String proxyImgUrl = "https://api.buxiangyao.link/51cg/img/?url=";

    private static final String siteUrl = "https://h25dz1.gxhfkyztz.com";
    private static final String cateUrl = siteUrl + "/category/";
    private static final String detailUrl = siteUrl + "/archives/";
    private static final String searchUrl = siteUrl + "/search?keywords=";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

//    private List<Vod> parseVods(Document doc) {
//        List<Vod> list = new ArrayList<>();
//        for (Element element : doc.select("article")) {
//            String pic = String.valueOf(element.select("script"));
//            String pattern = "'(https?://[^']+)";
//            Pattern regex = Pattern.compile(pattern);
//            Matcher matcher = regex.matcher(pic);
//            String PicAddress = "";
//            if (matcher.find()) {
//                PicAddress = proxyImgUrl + matcher.group(1);
//            } else {
//            }
//            String url = element.select("a").attr("href");
//            String name = element.select(".post-card-title").text();
//            String id = url.split("/")[2];
//            if (name != "" && url != ""){
//                list.add(new Vod(id, name, PicAddress));
//            }
//        }
//        return list;
//    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(20); // 创建一个线程池，最大并发数为10

        List<Callable<String>> tasks = new ArrayList<>(); // 用于存储所有的任务

        for (Element element : doc.select("article")) {
            String pic = String.valueOf(element.select("script"));
            String pattern = "'(https?://[^']+)";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(pic);
            String PicAddress = "";

            if (matcher.find()) {
                String imageUrl = matcher.group(1);
                tasks.add(() -> CgImageUtil.loadBackgroundImage(imageUrl)); // 创建一个任务，并将其添加到任务列表中
            }

            String url = element.select("a").attr("href");
            String name = element.select(".post-card-title").text();
            String id = url.split("/")[2];
            if (!name.isEmpty() && !url.isEmpty()) {
                list.add(new Vod(id, name, PicAddress));
            }
        }
        try {
            // 执行所有的任务，并获取结果
            List<Future<String>> futures = executorService.invokeAll(tasks);

            // 遍历任务结果，并将结果设置到对应的Vod对象中
            for (int i = 0; i < futures.size(); i++) {
                if (i < list.size()) { // 确保索引不超出列表范围
                    Vod vod = list.get(i);
                    String result = futures.get(i).get();
                    vod.setVodPic(result);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        executorService.shutdown(); // 关闭线程池
        return list;
    }
    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"wpcz","mrdg","rdsj","bkdg","whhl","xsxy","whmx"};
        String[] typeNameList = {"今日吃瓜","每日大瓜","热门吃瓜","必看大瓜","网红黑料","学生学校","明星黑料"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        Document doc = Jsoup.parse(OkHttp.string(siteUrl, getHeaders()));
        List<Vod> list = parseVods(doc);
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid + "/" + pg + "/";
        Document doc = Jsoup.parse(OkHttp.string(target, getHeaders()));
        List<Vod> list = parseVods(doc);
        Integer total = (Integer.parseInt(pg)+1)*20;
        return Result.string(Integer.parseInt(pg),Integer.parseInt(pg)+1,20,total,list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0)), getHeaders()));
        String playUrl = "";
        int index = 1;
        for (Element element : doc.select("div.dplayer")) {
            String play = element.attr("data-config");
            JSONObject jsonObject = new JSONObject(play);
            JSONObject video = jsonObject.getJSONObject("video");
            if (playUrl == ""){
                playUrl = "第" + index + "集$" + video.get("url");
            }else {
                playUrl = playUrl + "#第" + index + "集$" + video.get("url");
            }
            index++;
        }
        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String year = doc.select("meta[property=video:release_date]").attr("content");

        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodPlayFrom("Cg51");
        vod.setVodPlayUrl(playUrl);
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
