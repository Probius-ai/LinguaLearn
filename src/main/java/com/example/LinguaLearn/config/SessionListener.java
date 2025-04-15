package com.example.LinguaLearn.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.LinguaLearn.model.User;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * 세션 생성 및 소멸 이벤트를 감지하는 리스너
 */
@Component
public class SessionListener implements HttpSessionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);
    private static final String SESSION_USER_KEY = "user";
    
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        logger.debug("Session created: {}", session.getId());
        
        // 세션 타임아웃 설정 (30분 = 1800초)
        session.setMaxInactiveInterval(1800);
        
        // 세션 생성 시간 기록
        session.setAttribute("creationTime", System.currentTimeMillis());
        session.setAttribute("lastAccessTime", System.currentTimeMillis());
    }
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        
        // 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        
        if (user != null) {
            logger.info("Session destroyed for user: {}", user.getUid());
            
            // 세션 생존 시간 계산 (밀리초)
            long creationTime = session.getCreationTime();
            long currentTime = System.currentTimeMillis();
            long sessionLifetime = currentTime - creationTime;
            
            logger.info("Session lifetime: {} seconds", sessionLifetime / 1000);
            
            // 필요한 경우 세션 만료 처리 로직 추가
            // 예: 사용자 로그아웃 기록 저장
        } else {
            logger.debug("Session destroyed: {}", session.getId());
        }
    }
}