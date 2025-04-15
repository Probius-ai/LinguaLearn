package com.example.LinguaLearn.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.LinguaLearn.service.WordService;
import com.example.LinguaLearn.service.WordService.WordDto;

@RestController
@RequestMapping("/api/admin/words")
public class WordAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WordAdminController.class);
    
    @Autowired
    private WordService wordService;
    
    /**
     * 단어 목록 가져오기 (필터링 지원)
     */
    @GetMapping
    public ResponseEntity<?> getWords(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String level) {
        try {
            List<WordDto> words;
            
            if (language != null && level != null) {
                words = wordService.getWordsByLanguageAndLevel(language, level);
            } else if (language != null) {
                words = wordService.getWordsByLanguage(language);
            } else if (level != null) {
                words = wordService.getWordsByLevel(level);
            } else {
                words = wordService.getAllWords();
            }
            
            return ResponseEntity.ok(words);
        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "단어 목록을 불러오는 데 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 특정 언어와 레벨에 속한 단어 조회
     */
    @GetMapping("/{language}/{level}")
    public ResponseEntity<?> getWordsByLanguageAndLevel(
            @PathVariable String language,
            @PathVariable String level) {
        try {
            List<WordDto> words = wordService.getWordsByLanguageAndLevel(language, level);
            return ResponseEntity.ok(words);
        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "단어 목록을 불러오는 데 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * ID로 단어 조회
     */
    @GetMapping("/{language}/{level}/{id}")
    public ResponseEntity<?> getWordById(
            @PathVariable String language,
            @PathVariable String level,
            @PathVariable String id) {
        try {
            WordDto word = wordService.getWordById(language, level, id);
            if (word == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "ID가 " + id + "인 단어를 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(word);
        } catch (Exception e) {
            logger.error("단어 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "단어를 불러오는 데 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 단어 추가
     */
    @PostMapping
    public ResponseEntity<?> createWord(@RequestBody WordDto word) {
        try {
            // 필수 필드 검증
            if (word.getText() == null || word.getText().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "단어 텍스트는 필수 입력 항목입니다."));
            }
            
            if (word.getLanguage() == null || word.getLanguage().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "언어는 필수 입력 항목입니다."));
            }
            
            if (word.getLevel() == null || word.getLevel().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "레벨은 필수 입력 항목입니다."));
            }
            
            WordDto createdWord = wordService.createWord(word);
            if (createdWord == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "지원되지 않는 언어 또는 레벨입니다."));
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWord);
        } catch (Exception e) {
            logger.error("단어 추가 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "단어 추가에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 단어 업데이트
     */
    @PutMapping("/{language}/{level}/{id}")
    public ResponseEntity<?> updateWord(
            @PathVariable String language,
            @PathVariable String level,
            @PathVariable String id,
            @RequestBody WordDto word) {
        try {
            // 필수 필드 검증
            if (word.getText() == null || word.getText().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "단어 텍스트는 필수 입력 항목입니다."));
            }
            
            if (word.getLanguage() == null || word.getLanguage().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "언어는 필수 입력 항목입니다."));
            }
            
            if (word.getLevel() == null || word.getLevel().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "레벨은 필수 입력 항목입니다."));
            }
            
            // ID 설정 (URL에서 가져온 ID로 설정)
            word.setId(id);
            
            WordDto updatedWord = wordService.updateWord(language, level, id, word);
            if (updatedWord == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "ID가 " + id + "인 단어를 찾을 수 없거나 지원되지 않는 언어/레벨입니다."));
            }
            
            return ResponseEntity.ok(updatedWord);
        } catch (Exception e) {
            logger.error("단어 업데이트 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "단어 업데이트에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 단어 삭제
     */
    @DeleteMapping("/{language}/{level}/{id}")
    public ResponseEntity<?> deleteWord(
            @PathVariable String language,
            @PathVariable String level,
            @PathVariable String id) {
        try {
            boolean deleted = wordService.deleteWord(language, level, id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "ID가 " + id + "인 단어를 찾을 수 없거나 지원되지 않는 언어/레벨입니다."));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "단어가 성공적으로 삭제되었습니다."
            ));
        } catch (Exception e) {
            logger.error("단어 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "단어 삭제에 실패했습니다: " + e.getMessage()));
        }
    }
}