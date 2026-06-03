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

@Component
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    @Resource
    private OkHttpClient okHttpClient;

    public String get(String url) {
        return getWithRetry(url, 3, null);
    }

    // fix: timeoutSeconds现在真正生效，基于传入的超时时间设置OkHttpClient
    public String get(String url, int timeoutSeconds) {
        return getWithRetry(url, 3, timeoutSeconds);
    }

    // opt: 新增timeoutSeconds参数，支持动态超时设置
    private String getWithRetry(String url, int maxRetries, Integer timeoutSeconds) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", "https://quote.eastmoney.com/")
                .header("Connection", "keep-alive")
                .get()
                .build();

        // fix: 根据timeoutSeconds创建配置了超时的OkHttpClient实例
        OkHttpClient client = okHttpClient;
        if (timeoutSeconds != null) {
            client = okHttpClient.newBuilder()
                    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();
            logger.debug("使用自定义超时配置: {}s", timeoutSeconds);
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.info("HTTP请求尝试 {}/{}: {}", attempt, maxRetries, url);
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().string();
                    logger.info("HTTP请求成功，响应长度: {}", result.length());
                    return result;
                } else {
                    logger.warn("HttpUtil.get: url={}, 响应不成功, code={}", url, response.code());
                }
            } catch (IOException e) {
                logger.error("HTTP请求失败 (尝试 {}/{}): {}, error={}", attempt, maxRetries, url, e.getMessage(), e);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return null;
    }
}
