package com.example.LinguaLearn.security;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Firebase 인증 정보를 담는 커스텀 Authentication 토큰
 * FirebaseToken 대신 필요한 정보만 저장하여 직렬화 문제 해결
 */
public class FirebaseAuthenticationToken extends AbstractAuthenticationToken implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String uid;
    private final String email;
    private final String name;
    
    public FirebaseAuthenticationToken(String uid, String email, String name, 
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.uid = uid;
        this.email = email;
        this.name = name;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // 토큰 기반 인증은 자격 증명(비밀번호)이 없음
    }

    @Override
    public Object getPrincipal() {
        return uid; // 기본 식별자는 UID
    }
    
    public String getUid() {
        return uid;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getName() {
        return name;
    }
}