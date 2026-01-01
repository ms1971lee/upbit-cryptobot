package com.cryptobot.upbit.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateApiKeyRequest {

    @NotBlank(message = "Access Key는 필수입니다")
    private String upbitAccessKey;

    @NotBlank(message = "Secret Key는 필수입니다")
    private String upbitSecretKey;
}
