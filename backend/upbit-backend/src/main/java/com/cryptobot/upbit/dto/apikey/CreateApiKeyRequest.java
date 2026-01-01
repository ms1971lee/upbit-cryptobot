package com.cryptobot.upbit.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateApiKeyRequest {

    @NotBlank(message = "API 키 이름은 필수입니다")
    @Size(max = 100, message = "API 키 이름은 최대 100자까지 가능합니다")
    private String name;

    @NotBlank(message = "Access Key는 필수입니다")
    private String accessKey;

    @NotBlank(message = "Secret Key는 필수입니다")
    private String secretKey;

    private Boolean isActive = false;
}
