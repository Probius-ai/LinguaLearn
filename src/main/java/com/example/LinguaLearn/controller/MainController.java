package com.example.LinguaLearn.controller; // Adjust the package name as needed

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home() {
        // This returns the name of the Thymeleaf template file (without .html)
        // Spring Boot will look for src/main/resources/templates/main-page.html
        return "main-page";
    }

    // Add other mappings for your application if needed
}