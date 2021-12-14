package com.lyricist.server.controllers;

import com.lyricist.server.Util;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;



@RestController
public class ErrorControllerV implements ErrorController {
    @RequestMapping(value = "/error")
    String err() {
        return Util.readFile(new File("").getAbsolutePath() + "/src/main/resources/error.html");
    }
}
