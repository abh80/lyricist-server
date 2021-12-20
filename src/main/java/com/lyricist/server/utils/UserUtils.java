package com.lyricist.server.utils;

import com.lyricist.server.Util;
import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;

@Component
public class UserUtils {
    private final String site_secret;

    public UserUtils() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream("application.properties")) {
            properties.load(resourceStream);
        } catch (IOException e) {
            Util.error(e.getMessage());
        }
        this.site_secret = (String) properties.get("site_secret");
    }

    public static String generateUID() {
        int total_chars = 18;
        String chars = "abcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder uid = new StringBuilder();
        for (int i = 0; i <= total_chars; i++) {
            uid.append(chars.charAt((int) Math.floor(Math.random() * (chars.length() - 1))));
        }
        return uid.toString();
    }

    public static String generateSession() {
        int total_chars = 24;
        String chars = "abcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder str = new StringBuilder();
        for (int i = 0; i <= total_chars; i++) {
            str.append(chars.charAt((int) Math.floor(Math.random() * (chars.length() - 1))));
        }
        return str.toString();
    }

    public static String generateSession(int total_chars) {
        String chars = "abcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder str = new StringBuilder();
        for (int i = 0; i <= total_chars; i++) {
            str.append(chars.charAt((int) Math.floor(Math.random() * (chars.length() - 1))));
        }
        return str.toString();
    }

    public static boolean validateEmail(String email) {
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        return email.matches(regex);
    }

    public static String generateToken(String id) {
        id = new String(Base64.encodeBase64(id.getBytes()));
        String num = "1234567890";
        int total_len = 7;
        StringBuilder number_part = new StringBuilder();
        for (int i = 0; i <= total_len; i++) {
            number_part.append(num.charAt((int) Math.floor(Math.random() * (num.length() - 1))));
        }
        return id + "." + Instant.now().toEpochMilli() + "." + number_part;

    }

    public boolean verifyCaptchaResponse(String response, String ip_address) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
            post.setEntity(new StringEntity("secret=" + site_secret + "&response=" + response + "&remoteip=" + ip_address));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            HttpResponse res = httpClient.execute(post);
            if (res.getStatusLine().getStatusCode() != 200) {
                return false;
            }
            HttpEntity entity = res.getEntity();
            String json = EntityUtils.toString(entity, CharEncoding.UTF_8);
            JSONObject object = (JSONObject) new JSONParser().parse(json);
            return (boolean) object.get("success");
        } catch (IOException | org.json.simple.parser.ParseException e) {
            Util.error(e.getMessage());
            return false;
        }
    }
    public boolean verifyCaptchaResponse(String response) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost("https://www.google.com/recaptcha/api/siteverify");
            post.setEntity(new StringEntity("secret=" + site_secret + "&response=" + response));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            HttpResponse res = httpClient.execute(post);
            if (res.getStatusLine().getStatusCode() != 200) {
                return false;
            }
            HttpEntity entity = res.getEntity();
            String json = EntityUtils.toString(entity, CharEncoding.UTF_8);
            JSONObject object = (JSONObject) new JSONParser().parse(json);
            return (boolean) object.get("success");
        } catch (IOException | org.json.simple.parser.ParseException e) {
            Util.error(e.getMessage());
            return false;
        }
    }
}
