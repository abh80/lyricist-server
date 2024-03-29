package com.lyricist.server;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
    public static void Log(String message) {
        System.out.println(getTime() + Ansi.colorize(message, Attribute.BRIGHT_BLUE_TEXT()));
    }

    public static void success(String message) {
        System.out.println(getTime() + Ansi.colorize(message, Attribute.BRIGHT_GREEN_TEXT()));
    }

    public static void error(String message) {
        System.out.println(getTime() + Ansi.colorize(message, Attribute.BRIGHT_RED_TEXT()));
    }

    public static String getTime() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy.dd.MM | HH:mm:ss");
        return "[" + format.format(date) + "] ";
    }

    public static String readFile(String absolutePath) {
        try {
            InputStream stream =  new ClassPathResource(absolutePath).getInputStream();
            byte[] data = FileCopyUtils.copyToByteArray(stream);
            return  new String(data, StandardCharsets.UTF_8);

        } catch (IOException e) {
            error(e.getMessage());
            return "500 server ran into an error!";

        }
    }
}
