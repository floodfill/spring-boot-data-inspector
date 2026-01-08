package com.example.datainspector.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Serves the web UI
 */
@Controller
@RequestMapping("/data-inspector")
public class DataInspectorViewController {

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/data-inspector/ui";
    }

    @GetMapping(value = "/ui", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String ui() throws IOException {
        ClassPathResource resource = new ClassPathResource("data-inspector-ui.html");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
