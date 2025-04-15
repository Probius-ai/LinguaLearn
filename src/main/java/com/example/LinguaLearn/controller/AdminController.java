package com.example.LinguaLearn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.LinguaLearn.model.User;
import com.example.LinguaLearn.service.WordService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private WordService wordService;
    
    /**
     * 관리자 메인 페이지
     */
    @GetMapping
    public String adminHome(Model model, HttpSession session) {
        // 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/admin";
        }
        
        // TODO: 실제 구현에서는 관리자 권한 확인 필요
        
        model.addAttribute("user", user);
        return "admin/index";
    }
    
    /**
     * 단어 관리 페이지
     */
    @GetMapping("/words")
    public String wordManagement(Model model, HttpSession session) {
        // 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login?redirect=/admin/words";
        }
        
        // TODO: 실제 구현에서는 관리자 권한 확인 필요
        
        model.addAttribute("user", user);
        return "admin/word-management";
    }
}