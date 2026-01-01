package com.cryptobot.upbit.dto.apikey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDto {

    private Long id;
    private String name;
    private Boolean isActive;
    private String accessKeyMasked; // 마스킹된 access key (앞 4자리만 보여줌)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
