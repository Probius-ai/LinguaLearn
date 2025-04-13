package com.example.LinguaLearn.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/public/hello")
    public String publicHello() {
        return "Hello from Public Endpoint!";
    }

    @GetMapping("/api/secure/hello") // Secured endpoint
    public String secureHello() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName(); // Gets the UID from the token
        return "Hello, " + userId + "! You are accessing a secured endpoint.";
    }
}
