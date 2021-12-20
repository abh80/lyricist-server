package com.lyricist.server.controllers;

import com.lyricist.server.Util;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.*;

import java.io.File;


@CrossOrigin
@RestController
public class ErrorControllerV implements ErrorController {
    @RequestMapping(value = "/error")
    String err() {
        return Util.readFile("error.html");
    }
}
