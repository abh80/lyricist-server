package com.lyricist.server.utils;

import org.springframework.http.HttpStatus;

public class ErrorJson {
    public String message;
    public int statusCode;
    public String statusText;
    public ErrorCodes errorCode;

    public ErrorJson(String message, int statusCode, String statusText) {
        this.message = message;
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
    public ErrorJson(String message, int statusCode, String statusText , ErrorCodes errorCode) {
        this.message = message;
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.errorCode = errorCode;
    }
}

