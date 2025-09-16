package com.ai.agent.real.web.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/config", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConfigController {

    private static final File CONF_FILE = new File("agent-config.json");

    public static class SaveReq {
        public String content;
    }

}
