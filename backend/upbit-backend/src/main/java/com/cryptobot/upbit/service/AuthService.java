package com.cryptobot.upbit.service;

import com.cryptobot.upbit.config.JwtProperties;
import com.cryptobot.upbit.dto.auth.AuthResponse;
import com.cryptobot.upbit.dto.auth.LoginRequest;
import com.cryptobot.upbit.dto.auth.SignupRequest;
import com.cryptobot.upbit.dto.auth.UserDto;
import com.cryptobot.upbit.entity.User;
import com.cryptobot.upbit.exception.DuplicateEmailException;
import com.cryptobot.upbit.exception.InvalidCredentialsException;
import com.cryptobot.upbit.exception.UserNotFoundException;
import com.cryptobot.upbit.repository.UserRepository;
import com.cryptobot.upbit.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionService encryptionService;
    private final JwtProperties jwtProperties;

    /**
     * 회원가입
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        // 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        // 업비트 API 키 암호화 (있는 경우에만)
        String encryptedAccessKey = null;
        String encryptedSecretKey = null;

        if (StringUtils.hasText(request.getUpbitAccessKey())) {
            encryptedAccessKey = encryptionService.encrypt(request.getUpbitAccessKey());
        }

        if (StringUtils.hasText(request.getUpbitSecretKey())) {
            encryptedSecretKey = encryptionService.encrypt(request.getUpbitSecretKey());
        }

        // User 엔티티 생성 및 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(encryptedPassword)
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .upbitAccessKey(encryptedAccessKey)
                .upbitSecretKey(encryptedSecretKey)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        log.info("New user registered: {}", savedUser.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getId());

        // UserDto 생성
        UserDto userDto = convertToUserDto(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .user(userDto)
                .build();
    }

    /**
     * 로그인
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // 계정 활성화 확인
        if (!user.getEnabled()) {
            throw new InvalidCredentialsException("비활성화된 계정입니다");
        }

        log.info("User logged in: {}", user.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());

        // UserDto 생성
        UserDto userDto = convertToUserDto(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .user(userDto)
                .build();
    }

    /**
     * 현재 사용자 조회
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return convertToUserDto(user);
    }

    /**
     * User 엔티티를 UserDto로 변환
     */
    private UserDto convertToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .hasUpbitApiKey(StringUtils.hasText(user.getUpbitAccessKey())
                        && StringUtils.hasText(user.getUpbitSecretKey()))
                .build();
    }
}
