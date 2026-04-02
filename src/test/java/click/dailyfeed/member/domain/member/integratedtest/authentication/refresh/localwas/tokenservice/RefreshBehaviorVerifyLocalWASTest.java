package click.dailyfeed.member.domain.member.integratedtest.authentication.refresh.localwas.tokenservice;

import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.jwt.exception.InvalidTokenException;
import click.dailyfeed.member.domain.jwt.entity.RefreshToken;
import click.dailyfeed.member.domain.jwt.repository.jpa.RefreshTokenRepository;
import click.dailyfeed.member.domain.jwt.repository.jpa.TokenBlacklistRepository;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles({"local-test"})
@SpringBootTest
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "click.dailyfeed.feign..*"
        )
)
public class RefreshBehaviorVerifyLocalWASTest {

    @SpyBean
    private TokenService tokenService;

    @MockBean
    private JwtKeyHelper jwtKeyHelper;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private TokenBlacklistRepository tokenBlacklistRepository;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean(name = "stringRedisTemplate")
    private RedisTemplate<String, String> stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("(1) refreshToken 이 캐시 내 revokedRefreshKey 저장소에 존재하지 않을 경우 (정상)")
    void refreshTokens_Success_WhenNotRevoked() {
        // given
        String refreshTokenValue = "valid-refresh-token";
        String deviceInfo = "device";
        String ipAddress = "127.0.0.1";
        Long memberId = 1L;

        RefreshToken refreshToken = createRefreshToken(refreshTokenValue, memberId, LocalDateTime.now().plusDays(1));
        Member member = Member.newMember().password("password").roles("USER").build();
        ReflectionTestUtils.setField(member, "id", memberId);

        // 철회했던 refreshKey 가 아닐 경우를 가정 (revokedRefreshKey 내에 존재하는지 검사)
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // refreshToken 이 존재하는 정상 케이스를 가정
        when(refreshTokenRepository.findByTokenValueAndIsRevokedFalse(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        // 회원 데이터 존재 여부 검사 & 정상 케이스를 가정
        when(memberRepository.findByIdFetchJoin(memberId)).thenReturn(Collections.singletonList(member));
        // 만료기한이 여유가 충분한 시간을 return 하도록 지정
        when(jwtKeyHelper.generateAccessTokenExpiration()).thenReturn(new Date());
        // access token 이름은 new-access-token
        when(jwtKeyHelper.generateTokenWithJti(any(), anyString())).thenReturn("new-access-token");

        // when
        tokenService.refreshTokens(refreshTokenValue, deviceInfo, ipAddress);

        // then
        // 다음의 4개의 메서드들이 수행됨을 보장해야 함
        verify(tokenService).checkIfRevokedOrThrow(anyString());
        verify(refreshTokenRepository).findByTokenValueAndIsRevokedFalse(refreshTokenValue);
        verify(tokenService).revokeRefreshKeyOrThrow(eq(refreshToken), anyString());
        verify(memberRepository).findByIdFetchJoin(memberId);
    }

    @Test
    @DisplayName("(2) refreshToken 이 캐시 내 revokedRefreshKey 저장소에 존재할 경우 (에러 - 이미 철회했던 토큰)")
    void refreshTokens_ThrowException_WhenRevokedInRedis() {
        // given
        String refreshTokenValue = "revoked-token";
        String revokedRefreshKey = tokenService.revokedRefreshTokenKey(refreshTokenValue);
        // 철회된 토큰으로 /refresh에 요청이 온 경우를 가정
        when(stringRedisTemplate.hasKey(revokedRefreshKey)).thenReturn(true);

        // when & then
        // 이 경우 InvalidTokenException 을 throw 해야 함
        assertThrows(InvalidTokenException.class, () -> 
            tokenService.refreshTokens(refreshTokenValue, "device", "127.0.0.1"));
    }

    @Test
    @DisplayName("(1.1) refreshToken 을 refreshToken Repository 로 조회 시 존재하지 않는 토큰일 경우")
    void refreshTokens_ThrowException_WhenNotFoundInRepo() {
        // given
        String refreshTokenValue = "non-existent-token";
        // revoked token 은 아니지만
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // 존재하지 않는 refresh token일 경우를 가정
        when(refreshTokenRepository.findByTokenValueAndIsRevokedFalse(refreshTokenValue)).thenReturn(Optional.empty());

        // when & then
        // 이 경우 InvalidTokenException 을 throw 해야 함
        assertThrows(InvalidTokenException.class, () -> 
            tokenService.refreshTokens(refreshTokenValue, "device", "127.0.0.1"));
    }

    @Test
    @DisplayName("(1.2) 기간이 만료된 refreshToken 일 경우")
    void refreshTokens_ThrowException_WhenExpired() {
        // given
        String refreshTokenValue = "expired-token";
        Long memberId = 1L;
        // Expired 1 day ago
        RefreshToken expiredToken = createRefreshToken(refreshTokenValue, memberId, LocalDateTime.now().minusDays(1));
        // revoked 된 토큰이 아니면서
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // 기간이 만료된 refreshToken 일 경우
        when(refreshTokenRepository.findByTokenValueAndIsRevokedFalse(refreshTokenValue)).thenReturn(Optional.of(expiredToken));

        // when & then
        // 이 경우 InvalidTokenException 을 throw 해야 함
        assertThrows(InvalidTokenException.class, () -> 
            tokenService.refreshTokens(refreshTokenValue, "device", "127.0.0.1"));
    }

    @Test
    @DisplayName("(1.3) memberRepository 에서 memberId 를 조회 시 존재하지 않는 사용자일 경우")
    void refreshTokens_ThrowException_WhenMemberNotFound() {
        // given
        String refreshTokenValue = "valid-token-but-no-member";
        Long memberId = 999L;
        RefreshToken refreshToken = createRefreshToken(refreshTokenValue, memberId, LocalDateTime.now().plusDays(1));

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(refreshTokenRepository.findByTokenValueAndIsRevokedFalse(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        // 사용자가 DB에 존재하지 않을 경우
        when(memberRepository.findByIdFetchJoin(memberId)).thenReturn(Collections.emptyList());

        // when & then
        assertThrows(MemberNotFoundException.class, () -> 
            tokenService.refreshTokens(refreshTokenValue, "device", "127.0.0.1"));
    }

    private RefreshToken createRefreshToken(String value, Long memberId, LocalDateTime expiresAt) {
        return RefreshToken.create(
                "token-id",
                memberId,
                value,
                "jti",
                expiresAt,
                "device",
                "127.0.0.1"
        );
    }
}
