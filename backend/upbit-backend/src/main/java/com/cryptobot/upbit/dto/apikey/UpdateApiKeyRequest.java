package com.cryptobot.upbit.dto.apikey;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateApiKeyRequest {

    @Size(max = 100, message = "API 키 이름은 최대 100자까지 가능합니다")
    private String name;

    private String accessKey;

    private String secretKey;

    private Boolean isActive;
}
