package com.zhaoli.liojcodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 赵立
 */
@RestController("/")
public class MainController {
    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }
}
