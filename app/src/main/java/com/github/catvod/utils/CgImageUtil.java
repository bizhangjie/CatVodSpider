package com.github.catvod.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Security;

import android.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class CgImageUtil {
    private static final String IV = "97b60394abc2fbe1";
    private static final String KEY = "f5d965df75336270";

    private static String aesDecrypt(String word) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] srcBytes = Base64.decode(word, Base64.DEFAULT);
            byte[] keyBytes = KEY.getBytes("UTF-8");
            byte[] ivBytes = IV.getBytes("UTF-8");

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

            byte[] decryptedBytes = cipher.doFinal(srcBytes);
            return Base64.encodeToString(decryptedBytes,Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    public static void main(String[] args) {
//        String bgUrl = "https://pic.jcezlxm.cn/upload/upload/20240311/2024031120484810172.jpg";
//        String decryptedImage = loadBackgroundImage(bgUrl);
//        System.out.println(decryptedImage);
//    }

    public static String loadBackgroundImage(String bgUrl) {
        if (isCdnImg(bgUrl)) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(bgUrl)
                        .build();
                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();
                if (body != null) {
                    byte[] imageBytes = body.bytes();
                    String base64Str = Base64.encodeToString(imageBytes,Base64.DEFAULT);
                    System.out.println(base64Str);
                    String decryptedStr = aesDecrypt(base64Str);

                    // 将解密后的数据拼接为Data URL
                    String[] ary = bgUrl.split("\\.");
                    String base64st = "data:image/" + ary[ary.length - 1] + ";base64," + decryptedStr;
                    return base64st;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String base64st = "url(\"" + bgUrl + "\")";
            return base64st;
        }
        return null;
    }

    private static boolean isCdnImg(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.contains("/xiao/")) {
            return true;
        }
        if (path.contains("/upload/upload/")) {
            return true;
        }
        return false;
    }
}