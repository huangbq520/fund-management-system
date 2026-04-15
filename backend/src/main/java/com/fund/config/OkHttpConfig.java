package com.fund.config;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp Configuration
 */
@Configuration
public class OkHttpConfig {
    
    @Value("${okhttp.connect-timeout:5000}")
    private int connectTimeout;
    
    @Value("${okhttp.read-timeout:5000}")
    private int readTimeout;
    
    @Value("${okhttp.write-timeout:5000}")
    private int writeTimeout;
    
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .build();
    }
    
    @Bean
    public Request.Builder requestBuilder() {
        return new Request.Builder();
    }
}