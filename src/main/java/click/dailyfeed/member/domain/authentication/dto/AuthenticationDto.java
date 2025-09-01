package click.dailyfeed.member.domain.authentication.dto;

import lombok.*;

public class AuthenticationDto {
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class LoginRequest{
        private String email;
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class LoginResponse{
        private String statusCode;
        private String ok;
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SignupRequest{
        private String name;
        private String email;
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class SignupResponse{
        private String statusCode;
        private String ok;
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class LogoutResponse {
        private String statusCode;
        private String ok;
        private String reason;
    }
}
