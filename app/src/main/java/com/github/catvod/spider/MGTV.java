package com.github.catvod.spider;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MGTV extends Spider {
    
    protected JSONObject a;
    
    protected JSONObject f198q = new JSONObject();
    
    protected HashMap<String, String> dn = new HashMap<>();

    private String q(String str, String str2) {
        String str3;
        try {
            if (str2.startsWith("//")) {
                Uri parse = Uri.parse(str);
                str3 = parse.getScheme() + ":" + str2;
            } else if (str2.contains("://")) {
                return str2;
            } else {
                Uri parse2 = Uri.parse(str);
                str3 = parse2.getScheme() + "://" + parse2.getHost() + str2;
            }
            return str3;
        } catch (Exception e) {
            SpiderDebug.log(e);
            return str2;
        }
    }

    public String categoryContent(String str, String str2, boolean z, HashMap<String, String> hashMap) {
        String trim;
        try {
            String str3 = "https://pianku.api.mgtv.com/rider/list/msite/v2?platform=msite&channelId=" + str + "&pn=" + str2 + "&chargeInfo=&sort=c2";
            if (hashMap != null) {
                for (String str4 : hashMap.keySet()) {
                    if (hashMap.get(str4).trim().length() != 0) {
                        str3 = str3 + "&" + str4 + "=" + URLEncoder.encode(null);
                    }
                }
            }
            String m2007h = OkHttp.string(str3, dn(str3));
            JSONObject jSONObject = new JSONObject();
            try {
                JSONArray optJSONArray = new JSONObject(m2007h).optJSONObject("data").optJSONArray("hitDocs");
                JSONArray jSONArray = new JSONArray();
                for (int i = 0; i < optJSONArray.length(); i++) {
                    JSONObject optJSONObject = optJSONArray.optJSONObject(i);
                    String optString = optJSONObject.optString("title");
                    String q = q(str3, optJSONObject.optString("img"));
                    String optString2 = optJSONObject.optString("updateInfo");
                    if (optString2.equals("")) {
                        optString2 = optJSONObject.optString("subtitle");
                    }
                    JSONObject jSONObject2 = new JSONObject();
                    jSONObject2.put("vod_id", ("https://pcweb.api.mgtv.com/episode/list?size=5000&video_id=" + optJSONObject.optString("playPartId")) + "#" + q);
                    jSONObject2.put("vod_name", optString);
                    jSONObject2.put("vod_pic", q);
                    jSONObject2.put("vod_remarks", optString2);
                    jSONArray.put(jSONObject2);
                }
                jSONObject.put("list", jSONArray);
                jSONObject.put("page", str2);
                jSONObject.put("pagecount", Integer.MAX_VALUE);
                jSONObject.put("limit", 90);
                jSONObject.put("total", Integer.MAX_VALUE);
                jSONObject.put("list", jSONArray);
            } catch (Exception e) {
                SpiderDebug.log(e);
            }
            return jSONObject.toString(4);
        } catch (Exception e2) {
            SpiderDebug.log(e2);
            return "";
        }
    }

    public String detailContent(List<String> list) {
        try {
            String str = list.get(0).split("#")[0];
            JSONObject jSONObject = new JSONObject(OkHttp.string(str, dn(str))).getJSONObject("data");
            JSONObject jSONObject2 = new JSONObject();
            JSONObject optJSONObject = jSONObject.optJSONObject("info");
            jSONObject2.put("vod_id", list.get(0));
            jSONObject2.put("vod_name", optJSONObject.optString("title"));
            jSONObject2.put("vod_pic", q(str, list.get(0).split("#")[1]));
            jSONObject2.put("vod_content", optJSONObject.optString("desc"));
            JSONArray optJSONArray = jSONObject.optJSONArray("list");
            if (optJSONArray == null) {
                optJSONArray = new JSONArray();
                optJSONArray.put(jSONObject);
            }
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < optJSONArray.length(); i++) {
                JSONObject optJSONObject2 = optJSONArray.optJSONObject(i);
                arrayList.add(optJSONObject2.optString("t4") + "$" + optJSONObject2.optString("url"));
            }
            jSONObject2.put("vod_play_from", "mgtv");
            jSONObject2.put("vod_play_url", join("#", arrayList));
            JSONObject jSONObject3 = new JSONObject();
            JSONArray jSONArray = new JSONArray();
            jSONArray.put(jSONObject2);
            jSONObject3.put("list", jSONArray);
            return jSONObject3.toString(4);
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }
    
    protected HashMap<String, String> dn(String str) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("referer", "https://so.mgtv.com");
        hashMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36");
        return hashMap;
    }

    public String homeContent(boolean z) {
        try {
            JSONArray optJSONArray = new JSONObject(OkHttp.string("https://pianku.api.mgtv.com/rider/config/platformChannels/v1?platform=msite&abroad=0&_support=10000000", dn("https://pianku.api.mgtv.com/rider/config/platformChannels/v1?platform=msite&abroad=0&_support=10000000"))).optJSONArray("data");
            JSONArray jSONArray = new JSONArray();
            for (int i = 0; i < optJSONArray.length(); i++) {
                JSONObject optJSONObject = optJSONArray.optJSONObject(i);
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("type_name", optJSONObject.optString("channelName"));
                jSONObject.put("type_id", optJSONObject.optString("channelId"));
                jSONArray.put(jSONObject);
            }
            JSONObject jSONObject2 = new JSONObject();
            if (z) {
                jSONObject2.put("filters", this.a);
            }
            jSONObject2.put("class", jSONArray);
            try {
                JSONArray optJSONArray2 = new JSONObject(OkHttp.string("https://pianku.api.mgtv.com/rider/list/pcweb/v3?platform=pcweb&channelId=2&pn=1&chargeInfo=&sort=c2", dn("https://pianku.api.mgtv.com/rider/list/pcweb/v3?platform=pcweb&channelId=2&pn=1&chargeInfo=&sort=c2"))).optJSONObject("data").optJSONArray("hitDocs");
                JSONArray jSONArray2 = new JSONArray();
                for (int i2 = 0; i2 < optJSONArray2.length(); i2++) {
                    JSONObject optJSONObject2 = optJSONArray2.optJSONObject(i2);
                    String optString = optJSONObject2.optString("title");
                    String q = q("https://pianku.api.mgtv.com/rider/list/pcweb/v3?platform=pcweb&channelId=2&pn=1&chargeInfo=&sort=c2", optJSONObject2.optString("imgUrlH"));
                    String optString2 = optJSONObject2.optString("updateInfo");
                    JSONObject jSONObject3 = new JSONObject();
                    jSONObject3.put("vod_id", "https://pcweb.api.mgtv.com/episode/list?size=5000&video_id=" + optJSONObject2.optString("playPartId"));
                    jSONObject3.put("vod_name", optString);
                    jSONObject3.put("vod_pic", q);
                    jSONObject3.put("vod_remarks", optString2);
                    jSONArray2.put(jSONObject3);
                }
                jSONObject2.put("list", jSONArray2);
            } catch (Exception e) {
                SpiderDebug.log(e);
            }
            return jSONObject2.toString(4);
        } catch (Exception e2) {
            SpiderDebug.log(e2);
            return "";
        }
    }

    public String homeVideoContent() {
        try {
            JSONArray jSONArray = new JSONObject(OkHttp.string("https://www.mgtv.com/api.php/app/index_video?token=", dn("https://www.mgtv.com/api.php/app/index_video?token="))).getJSONArray("list");
            JSONArray jSONArray2 = new JSONArray();
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONArray jSONArray3 = jSONArray.getJSONObject(i).getJSONArray("vlist");
                for (int i2 = 0; i2 < jSONArray3.length() && i2 < 6; i2++) {
                    JSONObject jSONObject = jSONArray3.getJSONObject(i2);
                    JSONObject jSONObject2 = new JSONObject();
                    jSONObject2.put("vod_id", jSONObject.optString("vod_id"));
                    jSONObject2.put("vod_name", jSONObject.optString("vod_name"));
                    jSONObject2.put("vod_pic", jSONObject.optString("vod_pic"));
                    jSONObject2.put("vod_remarks", jSONObject.optString("vod_remarks"));
                    jSONArray2.put(jSONObject2);
                }
            }
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("list", jSONArray2);
            return jSONObject3.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
            return "";
        }
    }

    public void init(Context context) throws Exception {
        super.init(context);
        try {
            this.a = new JSONObject("{\"1\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"4\",\"n\":\"王牌综艺\"},{\"v\":\"5\",\"n\":\"大咖秀场\"},{\"v\":\"6\",\"n\":\"搞笑\"},{\"v\":\"7\",\"n\":\"情感\"},{\"v\":\"8\",\"n\":\"音乐\"},{\"v\":\"9\",\"n\":\"生活\"},{\"v\":\"179\",\"n\":\"亲子\"},{\"v\":\"170\",\"n\":\"旅游\"},{\"v\":\"171\",\"n\":\"时尚\"},{\"v\":\"173\",\"n\":\"真人秀\"},{\"v\":\"174\",\"n\":\"竞技\"},{\"v\":\"172\",\"n\":\"访谈\"},{\"v\":\"180\",\"n\":\"脱口秀\"}],\"key\":\"kind\"},{\"name\":\"地区\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"1\",\"n\":\"内地\"},{\"v\":\"2\",\"n\":\"港台\"},{\"v\":\"3\",\"n\":\"其他\"}],\"key\":\"area\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c1\",\"n\":\"最新\"},{\"v\":\"c2\",\"n\":\"最热\"},{\"v\":\"c4\",\"n\":\"知乎高分\"}],\"key\":\"sort\"}],\"2\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"14\",\"n\":\"甜蜜互宠\"},{\"v\":\"15\",\"n\":\"虐恋情深\"},{\"v\":\"16\",\"n\":\"青涩校园\"},{\"v\":\"17\",\"n\":\"仙侠玄幻\"},{\"v\":\"19\",\"n\":\"都市职场\"},{\"v\":\"148\",\"n\":\"古装\"},{\"v\":\"20\",\"n\":\"快意江湖\"},{\"v\":\"147\",\"n\":\"偶像\"},{\"v\":\"21\",\"n\":\"悬疑推理\"},{\"v\":\"22\",\"n\":\"家长里短\"},{\"v\":\"23\",\"n\":\"芒果出品\"},{\"v\":\"24\",\"n\":\"轻松搞笑\"},{\"v\":\"25\",\"n\":\"铁血战争\"},{\"v\":\"26\",\"n\":\"其他\"}],\"key\":\"kind\"},{\"name\":\"地区\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"10\",\"n\":\"内地\"},{\"v\":\"11\",\"n\":\"日韩\"},{\"v\":\"12\",\"n\":\"港台\"}],\"key\":\"area\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c2\",\"n\":\"最热\"},{\"v\":\"c1\",\"n\":\"最新\"},{\"v\":\"c4\",\"n\":\"知乎高分\"}],\"key\":\"sort\"},{\"name\":\"版本\",\"value\":[{\"v\":\"all\",\"n\":\"全部\"},{\"v\":\"2037\",\"n\":\"TV版\"},{\"v\":\"2038\",\"n\":\"特别版\"},{\"v\":\"2040\",\"n\":\"精华版\"},{\"v\":\"2036\",\"n\":\"未删减版\"}],\"key\":\"edition\"}],\"3\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"34\",\"n\":\"院线大片\"},{\"v\":\"175\",\"n\":\"爱情\"},{\"v\":\"176\",\"n\":\"喜剧\"},{\"v\":\"177\",\"n\":\"动作\"},{\"v\":\"178\",\"n\":\"科幻\"},{\"v\":\"39\",\"n\":\"青春\"},{\"v\":\"43\",\"n\":\"恐怖悬疑\"},{\"v\":\"44\",\"n\":\"战争\"},{\"v\":\"45\",\"n\":\"警匪\"},{\"v\":\"46\",\"n\":\"历史\"},{\"v\":\"47\",\"n\":\"歌舞\"},{\"v\":\"48\",\"n\":\"动画\"},{\"v\":\"50\",\"n\":\"其他\"}],\"key\":\"kind\"},{\"name\":\"资费\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"b1\",\"n\":\"免费\"},{\"v\":\"b2\",\"n\":\"VIP\"},{\"v\":\"b3\",\"n\":\"VIP用券\"},{\"v\":\"b4\",\"n\":\"付费点播\"}],\"key\":\"chargeInfo\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c2\",\"n\":\"最热\"},{\"v\":\"c1\",\"n\":\"最新\"},{\"v\":\"c4\",\"n\":\"知乎高分\"}],\"key\":\"sort\"}],\"106\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"106\",\"n\":\"时事\"},{\"v\":\"107\",\"n\":\"社会\"},{\"v\":\"108\",\"n\":\"文娱\"},{\"v\":\"109\",\"n\":\"军事\"}],\"key\":\"kind\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c1\",\"n\":\"最新\"},{\"v\":\"c2\",\"n\":\"最热\"}],\"key\":\"sort\"}],\"91\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"119\",\"n\":\"吐槽\"},{\"v\":\"120\",\"n\":\"恶搞\"},{\"v\":\"121\",\"n\":\"爆笑\"},{\"v\":\"122\",\"n\":\"奇趣\"},{\"v\":\"123\",\"n\":\"饭制\"},{\"v\":\"124\",\"n\":\"其他\"}],\"key\":\"kind\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c2\",\"n\":\"最热\"},{\"v\":\"c1\",\"n\":\"最新\"}],\"key\":\"sort\"}],\"50\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"60\",\"n\":\"恋爱少女\"},{\"v\":\"86\",\"n\":\"热血\"},{\"v\":\"62\",\"n\":\"搞笑\"},{\"v\":\"63\",\"n\":\"青春\"},{\"v\":\"64\",\"n\":\"魔幻仙侠\"},{\"v\":\"65\",\"n\":\"激燃运动\"},{\"v\":\"66\",\"n\":\"特摄\"},{\"v\":\"67\",\"n\":\"推理\"},{\"v\":\"68\",\"n\":\"亲子幼教\"},{\"v\":\"69\",\"n\":\"芒果出品\"},{\"v\":\"70\",\"n\":\"动漫音乐\"},{\"v\":\"71\",\"n\":\"经典\"},{\"v\":\"72\",\"n\":\"其他\"}],\"key\":\"kind\"},{\"name\":\"地区\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"52\",\"n\":\"内地\"},{\"v\":\"53\",\"n\":\"欧美\"},{\"v\":\"54\",\"n\":\"日韩\"},{\"v\":\"55\",\"n\":\"其他\"}],\"key\":\"area\"},{\"name\":\"版本\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"165\",\"n\":\"剧场版\"},{\"v\":\"57\",\"n\":\"TV版\"},{\"v\":\"166\",\"n\":\"OVA版\"},{\"v\":\"167\",\"n\":\"真人版\"}],\"key\":\"edition\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c2\",\"n\":\"最热\"},{\"v\":\"c1\",\"n\":\"最新\"}],\"key\":\"sort\"}],\"51\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"97\",\"n\":\"美食\"},{\"v\":\"98\",\"n\":\"文化\"},{\"v\":\"99\",\"n\":\"社会\"},{\"v\":\"100\",\"n\":\"历史\"},{\"v\":\"101\",\"n\":\"军事\"},{\"v\":\"102\",\"n\":\"人物\"},{\"v\":\"103\",\"n\":\"探索\"},{\"v\":\"104\",\"n\":\"自然\"},{\"v\":\"105\",\"n\":\"其他\"}],\"key\":\"kind\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c2\",\"n\":\"最热\"},{\"v\":\"c1\",\"n\":\"最新\"}],\"key\":\"sort\"}],\"20\":[{\"name\":\"类型\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"150\",\"n\":\"MV\"},{\"v\":\"151\",\"n\":\"影视原声\"},{\"v\":\"152\",\"n\":\"音乐节目\"},{\"v\":\"153\",\"n\":\"演唱会\"},{\"v\":\"154\",\"n\":\"颁奖礼\"},{\"v\":\"155\",\"n\":\"音乐现场\"},{\"v\":\"156\",\"n\":\"其他\"}],\"key\":\"kind\"},{\"name\":\"风格\",\"value\":[{\"v\":\"a1\",\"n\":\"全部\"},{\"v\":\"157\",\"n\":\"流行\"},{\"v\":\"158\",\"n\":\"Hip-hop\"},{\"v\":\"159\",\"n\":\"R&B\"},{\"v\":\"160\",\"n\":\"摇滚\"},{\"v\":\"161\",\"n\":\"民谣\"},{\"v\":\"162\",\"n\":\"爵士\"},{\"v\":\"163\",\"n\":\"古典\"},{\"v\":\"164\",\"n\":\"其他\"}],\"key\":\"musicStyle\"},{\"name\":\"排序\",\"value\":[{\"v\":\"c1\",\"n\":\"最新\"},{\"v\":\"c2\",\"n\":\"最热\"}],\"key\":\"sort\"}]}");
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
    }

    public String join(@NonNull CharSequence charSequence, @NonNull Iterable iterable) {
        Iterator it = iterable.iterator();
        if (it.hasNext()) {
            StringBuilder sb = new StringBuilder();
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(charSequence);
                sb.append(it.next());
            }
            return sb.toString();
        }
        return "";
    }

    public String playerContent(String str, String str2, List<String> list) {
        try {
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("parse", 1);
                jSONObject.put("playUrl", "");
                jSONObject.put("url", "https://www.mgtv.com" + str2);
                return jSONObject.toString();
            } catch (Exception e) {
                SpiderDebug.log(e);
                return jSONObject.toString();
            }
        } catch (Exception e2) {
            SpiderDebug.log(e2);
            return "";
        }
    }

    public java.lang.String searchContent(java.lang.String r18, boolean r19) {
        throw new UnsupportedOperationException("Method not decompiled: com.github.catvod.spider.MGTV.searchContent(java.lang.String, boolean):java.lang.String");
    }
}
