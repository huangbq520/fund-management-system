package com.fund.util;

import com.alibaba.fastjson2.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
public class BaiduOcrUtil {

    private static final Logger logger = LoggerFactory.getLogger(BaiduOcrUtil.class);
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
    private static final String REDIS_KEY = "baidu:access_token";
    private static final long TOKEN_TTL_SEC = 29 * 24 * 3600L;

    @Value("${baidu.ocr.api-key}")
    private String apiKey;

    @Value("${baidu.ocr.secret-key}")
    private String secretKey;

    @Resource
    private OkHttpClient okHttpClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final OkHttpClient apiClient = new OkHttpClient().newBuilder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build();

    public String getAccessToken() throws IOException {
        try {
            String cached = stringRedisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null && !cached.isEmpty()) {
                return cached;
            }
        } catch (Exception e) {
            logger.warn("Redis 读取 AccessToken 缓存失败: {}", e.getMessage());
        }

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType,
            "grant_type=client_credentials&client_id=" + apiKey
            + "&client_secret=" + secretKey);
        Request request = new Request.Builder()
            .url(TOKEN_URL)
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();

        logger.info("请求百度 AccessToken...");
        try (Response response = apiClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            logger.info("百度 AccessToken 响应: code={}, body={}", response.code(),
                respBody.length() > 200 ? respBody.substring(0, 200) + "..." : respBody);

            if (!response.isSuccessful()) {
                logger.error("百度 AccessToken 请求失败, HTTP code={}, body={}", response.code(), respBody);
                return null;
            }

            JSONObject json = JSONObject.parseObject(respBody);
            String token = json.getString("access_token");
            if (token != null && !token.isEmpty()) {
                try {
                    stringRedisTemplate.opsForValue().set(REDIS_KEY, token, TOKEN_TTL_SEC, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("Redis 缓存 AccessToken 失败: {}", e.getMessage());
                }
                logger.info("百度 AccessToken 获取成功");
                return token;
            }

            logger.error("百度 AccessToken 响应中未找到 access_token 字段: {}", respBody);
            return null;
        }
    }

    public String recognizeText(byte[] imageBytes) {
        try {
            String token = getAccessToken();
            if (token == null) {
                logger.error("获取百度 AccessToken 失败，无法进行 OCR 识别");
                return null;
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String encodedImage = URLEncoder.encode(base64Image, "UTF-8");

            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType,
                "image=" + encodedImage
                + "&detect_direction=false"
                + "&detect_language=false"
                + "&paragraph=false"
                + "&probability=false");

            Request request = new Request.Builder()
                .url(OCR_URL + "?access_token=" + token)
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();

            logger.info("请求百度 OCR 识别, 图片大小={} bytes", imageBytes.length);
            try (Response response = apiClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String respBody = response.body().string();
                    logger.info("百度 OCR 响应: code={}, bodyLen={}", response.code(), respBody.length());
                    if (response.isSuccessful()) {
                        return respBody;
                    }
                    logger.error("百度 OCR 请求失败, HTTP code={}, body={}",
                        response.code(), respBody.length() > 300 ? respBody.substring(0, 300) : respBody);
                } else {
                    logger.error("百度 OCR 响应 body 为空");
                }
            }
        } catch (IOException e) {
            logger.error("百度 OCR 请求异常: {}", e.getMessage(), e);
        }
        return null;
    }
}
