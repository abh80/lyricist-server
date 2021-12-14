package com.lyricist.server.utils;

import org.apache.tomcat.util.codec.binary.Base64;

import java.time.Instant;
import java.util.Date;

public class UserUtils {
    public static String generateUID() {
        int total_chars = 18;
        String chars = "abcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder uid = new StringBuilder();
        for (int i = 0; i <= total_chars; i++) {
            uid.append(chars.charAt((int) Math.floor(Math.random() * (chars.length() - 1))));
        }
        return uid.toString();
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
}
