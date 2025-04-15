package com.example.LinguaLearn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository.FlushMode;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800) // 30분 세션 타임아웃 설정
public class SpringSessionConfig extends AbstractHttpSessionApplicationInitializer {
    
    @Value("${spring.session.jdbc.table-name:SPRING_SESSION}")
    private String tableName;
    
    @Bean
    public LobHandler lobHandler() {
        return new DefaultLobHandler();
    }
    
    @Bean
    @SpringSessionDataSource
    public JdbcIndexedSessionRepository sessionRepository(
            DataSource dataSource, 
            JdbcOperations jdbcOperations,
            PlatformTransactionManager transactionManager) {
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        JdbcIndexedSessionRepository repository = new JdbcIndexedSessionRepository(
                jdbcOperations, transactionTemplate);
        
        // 세션 테이블 이름 설정
        repository.setTableName(tableName);
        
        repository.setFlushMode(JdbcIndexedSessionRepository.FlushMode.ON_SAVE);
        
        // 세션 정리 간격 설정 (30분)
        repository.setCleanupCron("0 */30 * * * *");
        
        // 세션 저장 시 락 획득 시도 시간 설정 (3초)
        repository.setSaveMode(JdbcIndexedSessionRepository.SaveMode.ALWAYS);
        
        return repository;
    }
}