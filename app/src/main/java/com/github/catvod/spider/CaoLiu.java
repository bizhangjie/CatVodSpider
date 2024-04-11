package com.github.catvod.spider;

import android.util.Base64;

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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CaoLiu extends Spider {

    private static final String siteUrl = "https://cl2404a55d.top";
    private static final String cateUrl = siteUrl + "/thread.php?fid=";
    private static final String detailUrl = siteUrl + "/video.php?tid=";
    private static final String searchUrl = "https://api.3bmmjla.life/Api/getSearch";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        return headers;
    }

    private HashMap<String, String> getCookie() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Util.CHROME);
        headers.put("Cookie", "_gid=GA1.2.248781789.1712806176; a2640_winduser=DgUDBlJWATlQXwgIVQACVV0FAgoCUFUAV1RSUQ4ABgkDUQlVVlAJUWs%3D; a2640_ck_info=%2F%09; a2640_appuser=DAQFUQZTCj1WXAANBQlVV1QAAAsEBwEFXFBUUgYFVgBUUwBQVFEPBj8%3D; a2640_ol_offset=51493; a2640_threadlog=%2C6%2C; a2640_readlog=%2C2290382%2C; a2640_lastpos=other; _ga_KCKJL9NRY7=GS1.1.1712806174.1.1.1712806229.0.0.0; a2640_lastvisit=13%091712806229%09%2Fdomainjs.php%3Ftype%3D3%26t%3D094; _ga_3ZHR4YBDVK=GS1.1.1712806174.1.1.1712806233.0.0.0; _ga_QTT9CLMQSW=GS1.1.1712806175.1.1.1712806234.0.0.0; _ga_255J96M2SG=GS1.1.1712806174.1.1.1712806235.0.0.0; _ga_LQZNZ7TBVX=GS1.1.1712806174.1.1.1712806237.0.0.0; _ga=GA1.2.999144089.1712806174; _gat_gtag_UA_207642113_1=1");
        return headers;
    }

    private static final int THREAD_POOL_SIZE = 25;

    public List<Vod> parseHtml(Document document) {
        List<Vod> list = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Vod>> futures = new ArrayList<>();

        Elements elements = document.select("div.vv-box");
        for (Element element : elements) {
            Future<Vod> future = executorService.submit(() -> {
                try {
                    String pic = element.select("img").attr("data-aes");
                    String id = element.select("img").attr("alt");
                    // 获取图片进行解密
                    String string = OkHttp.string(pic);
                    String picView = aesDecrypt(string);
                    String name = "看圖片";
                    return new Vod(id, name, picView);
                } catch (Exception e) {
                    // 处理异常情况
                    return null;
                }
            });
            futures.add(future);
        }

        for (Future<Vod> future : futures) {
            try {
                Vod vod = future.get();
                if (vod != null) {
                    list.add(vod);
                }
            } catch (Exception e) {
                // 处理异常情况
            }
        }
        executorService.shutdown();
        return list;
    }

    private static final String IV = "IMGy92137kxhxabI";
    private static final String KEY = "I884417AYxOK0123";

    // 解密方法
    private String aesDecrypt(String str) {
        try {
            byte[] ivBytes = IV.getBytes("UTF-8");
            byte[] keyBytes = KEY.getBytes("UTF-8");

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

            byte[] decryptedBytes = cipher.doFinal(Base64.decode(str, Base64.DEFAULT));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        String[] typeIdList = {"57", "33", "47", "6", "7", "2", "3", "4", "5", "48"};
        String[] typeNameList = {"特享影視", "影視專區", "草榴黑料記", "國產原創區", "中字原創區", "亞洲無碼區", "亞洲有碼區", "歐美原創區", "動漫原創區", "ASMR視訊區"};
        for (int i = 0; i < typeNameList.length; i++) {
            classes.add(new Class(typeIdList[i], typeNameList[i]));
        }
        List<Vod> list = new ArrayList<>();
        return Result.string(classes, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String target = cateUrl + tid + "&page=" + pg;
        Document doc = Jsoup.parse(OkHttp.string(target, getCookie()));
        // 只有图片模版
        if ("57".equals(tid)) {
            List<Vod> list = parseHtml(doc);
            Integer total = (Integer.parseInt(pg) + 1) * 20;
            return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 20, total, list);
        }
        List<Vod> list = new ArrayList<>();
        // 图文结合模版
        if ("47".equals(tid)) {
            for (Element element : doc.select("div.url_linkkarl")) {
                String pic = element.select("img").attr("data-aes");
                String href = element.attr("data-url").replace("read.php?tid=", "").split("&")[0];
                String name = element.select("h2").text();
                // 获取图片进行解密
//                String string = OkHttp.string(pic);
//                String picView = aesDecrypt(string);
                list.add(new Vod(href, name, pic));
            }
        }
        // 文字列表模版
        else {
            for (Element element : doc.select("td.tal")) {
                String id = element.select("a").attr("href").replace("read.php?tid=", "").split("&")[0];
                String name = element.select("a").text();
                list.add(new Vod(id, name, ""));
            }
        }
        Integer total = (Integer.parseInt(pg) + 1) * 100;
        return Result.string(Integer.parseInt(pg), Integer.parseInt(pg) + 1, 100, total, list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(detailUrl.concat(ids.get(0))));
        String name = doc.select("title").text().replace(" -  | 草榴社區", "");

        Pattern pattern = Pattern.compile("url: '(.*?)',");
        Matcher matcher = pattern.matcher(doc.html());
        String PlayUrl = "";
        // 提取匹配到的内容
        if (matcher.find()) {
            PlayUrl = matcher.group(1);
        }
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(name);
        vod.setVodPlayFrom("草榴");
        vod.setVodPlayUrl("播放$" + PlayUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String target = searchUrl;
        Map<String, String> params = new HashMap<>();
        params.put("className", "ed5315ea37ade2181edbd8b27b3fc881");
        params.put("keyword", key);
        params.put("page", "1");
        params.put("limit", "24");
        String data = OkHttp.post(target, params);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        JsonArray array = jsonObject.getAsJsonArray("data");
        List<Vod> list = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String title = object.get("title").toString().replace("\"", "");
            String pic = "https://3bmmaeh.life/pic" + object.get("titlepic").toString().replace("\"", "");
            String id = object.get("titleurl").toString().replace("\"", "");
            list.add(new Vod(id, title, pic));
        }

        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).header(getHeaders()).string();
    }
}
