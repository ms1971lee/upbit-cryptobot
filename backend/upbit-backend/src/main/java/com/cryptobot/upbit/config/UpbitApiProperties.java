package com.cryptobot.upbit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "upbit.api")
public class UpbitApiProperties {

    private String baseUrl;
    private Map<String, ApiKey> keys;

    @Getter
    @Setter
    public static class ApiKey {
        private String accessKey;
        private String secretKey;
    }
}
