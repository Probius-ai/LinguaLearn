package com.example.LinguaLearn.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("password")
                .build();
    }
}