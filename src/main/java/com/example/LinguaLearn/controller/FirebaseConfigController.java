package com.example.LinguaLearn.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/firebase")
public class FirebaseConfigController {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfigController.class);
    private final ObjectMapper objectMapper;

    @Autowired
    public FirebaseConfigController(ObjectMapper objectMapper) {
        // Spring Boot가 자동으로 ObjectMapper 빈을 주입해줍니다.
        this.objectMapper = objectMapper;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getFirebaseConfig() {
        try {
            // ClassPathResource를 사용하여 resources 폴더 아래의 파일을 읽습니다.
            ClassPathResource resource = new ClassPathResource("firebase-client-config.json");
            InputStream inputStream = resource.getInputStream();

            // Jackson ObjectMapper를 사용하여 JSON 파일을 Map<String, String>으로 변환합니다.
            Map<String, String> configMap = objectMapper.readValue(inputStream, new TypeReference<Map<String, String>>() {});

            logger.info("Successfully loaded Firebase client config from JSON file.");
            return ResponseEntity.ok(configMap);

        } catch (Exception e) {
            logger.error("Failed to load Firebase client config from JSON file", e);
            // 오류 발생 시 빈 Map 또는 에러 응답을 반환합니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }
}
