package com.fund.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Request Utility
 * 超时配置：5秒
 */
@Component
public class HttpUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
    @Resource
    private OkHttpClient okHttpClient;
    
    /**
     * Send GET request
     * 超时时间：5秒
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
    
    /**
     * Send GET request with custom timeout
     * @param url 请求URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 响应内容
     */
    public String get(String url, int timeoutSeconds) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                logger.warn("HttpUtil.get: url={}, response不成功, code={}", url, response.code());
            }
        } catch (IOException e) {
            logger.error("HTTP request failed: {}, error: {}", url, e.getMessage());
        }
        return null;
    }
}