package com.example.LinguaLearn.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 세션 충돌 방지를 위한 필터
 * 동시 요청 처리 시 세션 속성 충돌을 방지하기 위한 목적
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConcurrentSessionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentSessionFilter.class);
    private static final String PROCESSING_SESSION_ATTRIBUTE = "CONCURRENT_SESSION_PROCESSING";
    
    private final SessionRepository<? extends Session> sessionRepository;
    
    public ConcurrentSessionFilter(SessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        // 세션이 없으면 그냥 진행
        if (session == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String sessionId = session.getId();
        
        try {
            // 세션 처리 중 표시
            synchronized (ConcurrentSessionFilter.class) {
                session.setAttribute(PROCESSING_SESSION_ATTRIBUTE, true);
            }
            
            // 필터 체인 계속 진행
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error during session processing for session ID: " + sessionId, e);
            throw e;
        } finally {
            try {
                // 세션 처리 완료 표시
                synchronized (ConcurrentSessionFilter.class) {
                    if (session != null && request.isRequestedSessionIdValid()) {
                        session.removeAttribute(PROCESSING_SESSION_ATTRIBUTE);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error cleaning up session processing flag: " + e.getMessage());
            }
        }
    }
}