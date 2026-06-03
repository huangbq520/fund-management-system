package com.fund.config;

import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Value("${okhttp.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${okhttp.read-timeout:30000}")
    private int readTimeout;

    @Value("${okhttp.write-timeout:30000}")
    private int writeTimeout;

    @Bean
    public OkHttpClient okHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] c, String a) {}
                    @Override public void checkServerTrusted(X509Certificate[] c, String a) {}
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
                .followRedirects(true)
                .followSslRedirects(true)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectionSpecs(Arrays.asList(ConnectionSpec.COMPATIBLE_TLS))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure OkHttpClient", e);
        }
    }

    @Bean
    public Request.Builder requestBuilder() {
        return new Request.Builder();
    }
}
