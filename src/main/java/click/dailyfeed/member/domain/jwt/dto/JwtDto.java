package click.dailyfeed.member.domain.jwt.dto;

import lombok.*;

import java.util.Date;

public class JwtDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UserDetails{
        private Long id;
        private Date expiration;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private Long accessTokenExpiresIn;  // seconds
        private Long refreshTokenExpiresIn;  // seconds
    }

    /**
     * 토큰 생성 요청 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenGenerationRequest {
        private UserDetails userDetails;
        private String jti;
        private Date expirationDate;
    }

    /**
     * 토큰 검증 결과 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenValidationResult {
        private boolean valid;
        private String jti;
        private Long memberId;
        private String email;
        private Date expirationDate;
        private String errorMessage;
    }

}
