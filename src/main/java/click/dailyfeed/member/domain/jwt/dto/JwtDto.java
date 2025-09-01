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
        private String email;
        private String password;
        private Date expiration;
    }
}
