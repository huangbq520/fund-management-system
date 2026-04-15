package com.fund.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * HTTP Request Utility
 */
@Component
public class HttpUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
    @Resource
    private OkHttpClient okHttpClient;
    
    /**
     * Send GET request
     */
    public String get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            logger.error("HTTP request failed: {}, error: {}", url, e.getMessage());
        }
        return null;
    }
}