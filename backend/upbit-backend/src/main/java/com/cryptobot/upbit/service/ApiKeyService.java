package com.cryptobot.upbit.service;

import com.cryptobot.upbit.dto.apikey.ApiKeyDto;
import com.cryptobot.upbit.dto.apikey.CreateApiKeyRequest;
import com.cryptobot.upbit.dto.apikey.UpdateApiKeyRequest;
import com.cryptobot.upbit.entity.ApiKey;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.exception.UserNotFoundException;
import com.cryptobot.upbit.repository.ApiKeyRepository;
import com.cryptobot.upbit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    /**
     * 사용자의 모든 API 키 조회
     */
    @Transactional(readOnly = true)
    public List<ApiKeyDto> getAllApiKeys(String email) {
        User user = getUserByEmail(email);
        List<ApiKey> apiKeys = apiKeyRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        log.info("=== Get All API Keys ===");
        log.info("User: {}, Found {} API keys", email, apiKeys.size());

        List<ApiKeyDto> dtos = apiKeys.stream()
                .map(apiKey -> {
                    ApiKeyDto dto = convertToDto(apiKey);
                    log.info("API Key - ID: {}, Name: '{}', IsActive: {}, AccessKeyMasked: {}",
                             dto.getId(), dto.getName(), dto.getIsActive(), dto.getAccessKeyMasked());
                    return dto;
                })
                .collect(Collectors.toList());

        return dtos;
    }

    /**
     * API 키 생성
     */
    @Transactional
    public ApiKeyDto createApiKey(String email, CreateApiKeyRequest request) {
        User user = getUserByEmail(email);

        // 활성화 요청 시, 기존 활성화된 키 비활성화
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateAllApiKeys(user.getId());
        }

        // API 키 암호화
        String encryptedAccessKey = encryptionService.encrypt(request.getAccessKey());
        String encryptedSecretKey = encryptionService.encrypt(request.getSecretKey());

        // API 키 생성
        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .name(request.getName())
                .accessKey(encryptedAccessKey)
                .secretKey(encryptedSecretKey)
                .isActive(request.getIsActive())
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        log.info("API key created for user: {}, name: {}", user.getEmail(), request.getName());

        return convertToDto(savedApiKey);
    }

    /**
     * API 키 수정
     */
    @Transactional
    public ApiKeyDto updateApiKey(String email, Long apiKeyId, UpdateApiKeyRequest request) {
        log.info("=== API Key Update Request ===");
        log.info("User: {}, API Key ID: {}", email, apiKeyId);
        log.info("Request - Name: {}, AccessKey: {}, SecretKey: {}, IsActive: {}",
                 request.getName(),
                 request.getAccessKey() != null ? "PROVIDED" : "null",
                 request.getSecretKey() != null ? "PROVIDED" : "null",
                 request.getIsActive());

        User user = getUserByEmail(email);
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(apiKeyId, user.getId())
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다"));

        log.info("Before Update - Name: {}, IsActive: {}", apiKey.getName(), apiKey.getIsActive());

        // 이름 수정
        if (StringUtils.hasText(request.getName())) {
            log.info("Updating name from '{}' to '{}'", apiKey.getName(), request.getName());
            apiKey.setName(request.getName());
        } else {
            log.info("Name not updated (no value provided)");
        }

        // Access Key 수정
        if (StringUtils.hasText(request.getAccessKey())) {
            log.info("Updating access key");
            String encryptedAccessKey = encryptionService.encrypt(request.getAccessKey());
            apiKey.setAccessKey(encryptedAccessKey);
        } else {
            log.info("Access key not updated (no value provided)");
        }

        // Secret Key 수정
        if (StringUtils.hasText(request.getSecretKey())) {
            log.info("Updating secret key");
            String encryptedSecretKey = encryptionService.encrypt(request.getSecretKey());
            apiKey.setSecretKey(encryptedSecretKey);
        } else {
            log.info("Secret key not updated (no value provided)");
        }

        // 활성화 상태 수정
        if (request.getIsActive() != null) {
            log.info("Updating isActive from {} to {}", apiKey.getIsActive(), request.getIsActive());
            if (Boolean.TRUE.equals(request.getIsActive())) {
                deactivateAllApiKeys(user.getId());
            }
            apiKey.setIsActive(request.getIsActive());
        } else {
            log.info("IsActive not updated (no value provided)");
        }

        ApiKey updatedApiKey = apiKeyRepository.save(apiKey);

        log.info("After Update - Name: {}, IsActive: {}", updatedApiKey.getName(), updatedApiKey.getIsActive());
        log.info("API key updated successfully for user: {}, id: {}", user.getEmail(), apiKeyId);

        return convertToDto(updatedApiKey);
    }

    /**
     * API 키 삭제
     */
    @Transactional
    public void deleteApiKey(String email, Long apiKeyId) {
        User user = getUserByEmail(email);
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(apiKeyId, user.getId())
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다"));

        apiKeyRepository.delete(apiKey);

        log.info("API key deleted for user: {}, id: {}", user.getEmail(), apiKeyId);
    }

    /**
     * API 키 활성화
     */
    @Transactional
    public ApiKeyDto activateApiKey(String email, Long apiKeyId) {
        User user = getUserByEmail(email);
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(apiKeyId, user.getId())
                .orElseThrow(() -> new RuntimeException("API 키를 찾을 수 없습니다"));

        // 기존 활성화된 키 비활성화
        deactivateAllApiKeys(user.getId());

        // 현재 키 활성화
        apiKey.setIsActive(true);
        ApiKey updatedApiKey = apiKeyRepository.save(apiKey);

        log.info("API key activated for user: {}, id: {}", user.getEmail(), apiKeyId);

        return convertToDto(updatedApiKey);
    }

    /**
     * 활성화된 API 키 조회
     */
    @Transactional(readOnly = true)
    public ApiKeyDto getActiveApiKey(String email) {
        User user = getUserByEmail(email);
        return apiKeyRepository.findByUserIdAndIsActiveTrue(user.getId())
                .map(this::convertToDto)
                .orElse(null);
    }

    /**
     * 모든 API 키 비활성화
     */
    private void deactivateAllApiKeys(Long userId) {
        List<ApiKey> activeKeys = apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        activeKeys.forEach(key -> key.setIsActive(false));
        apiKeyRepository.saveAll(activeKeys);
    }

    /**
     * 이메일로 사용자 조회
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }

    /**
     * ApiKey를 ApiKeyDto로 변환
     */
    private ApiKeyDto convertToDto(ApiKey apiKey) {
        // Access Key 마스킹 (앞 4자리만 보여줌)
        String decryptedAccessKey = encryptionService.decrypt(apiKey.getAccessKey());
        String maskedAccessKey = maskAccessKey(decryptedAccessKey);

        return ApiKeyDto.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .isActive(apiKey.getIsActive())
                .accessKeyMasked(maskedAccessKey)
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .build();
    }

    /**
     * Access Key 마스킹
     */
    private String maskAccessKey(String accessKey) {
        if (accessKey == null || accessKey.length() <= 4) {
            return "****";
        }
        return accessKey.substring(0, 4) + "****" + accessKey.substring(accessKey.length() - 4);
    }
}
