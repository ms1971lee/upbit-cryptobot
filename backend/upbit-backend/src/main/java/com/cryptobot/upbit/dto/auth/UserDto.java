package com.cryptobot.upbit.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserDto {

    private Long id;

    private String email;

    private String username;

    private String phoneNumber;

    private boolean hasUpbitApiKey; // API 키 존재 여부만 전달 (보안) - deprecated

    private long apiKeyCount; // 등록된 API 키 개수
}
