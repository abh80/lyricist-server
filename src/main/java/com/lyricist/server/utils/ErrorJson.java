package com.lyricist.server.utils;

public class ErrorJson {
    public String message;
    public int statusCode;
    public String statusText;

    public ErrorJson(String message, int statusCode, String statusText) {
        this.message = message;
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
}
