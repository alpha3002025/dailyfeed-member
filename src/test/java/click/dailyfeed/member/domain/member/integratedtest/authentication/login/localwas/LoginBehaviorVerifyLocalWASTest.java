package click.dailyfeed.member.domain.member.integratedtest.authentication.login.localwas;

import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.service.AuthenticationService;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.jpa.MemberProfileRepository;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;

/// Note : Production 에서는 사용하지 마세요.
/// 로컬에서 테스트를 위한 회원 데이터를 넣는 테스트 코드
@ActiveProfiles({"local-test"})
@SpringBootTest
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "click.dailyfeed.feign..*"
        )
)
public class LoginBehaviorVerifyLocalWASTest {

    @MockitoSpyBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private MemberProfileRepository memberProfileRepository;

    @MockitoBean
    private JwtKeyHelper jwtKeyHelper;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private MemberProfileMapper memberProfileMapper;

    @Test
    @DisplayName("로그인 행위 검증 - 정상 요청 시 필수 로직들이 순서대로 호출되는지 확인")
    void login_Behavior_Verification() {
        // Given
        AuthenticationDto.LoginRequest loginRequest = AuthenticationDto.LoginRequest.builder()
                .email("test@dailyfeed.com")
                .password("password123!")
                .build();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // 1. Member 조회 Mocking
        Member member = Mockito.mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getPassword()).willReturn("encrypted-password");
        given(memberRepository.findFirstByEmailFetchJoin(anyString())).willReturn(Optional.of(member));

        // 2. 비밀번호 체크 로직 Mocking (실제 PasswordEncoder를 타지 않도록 doNothing 설정)
        doNothing().when(authenticationService).checkIfPasswordMatchesOrThrow(anyString(), anyString());

        // 3. 만료 시간 생성 Mocking
        Date expirationDate = new Date();
        given(jwtKeyHelper.generateAccessTokenExpiration()).willReturn(expirationDate);

        // 4. 토큰 쌍 생성 Mocking (null 매칭을 위해 any() 사용)
        TokenService.TokenPair tokenPair = new TokenService.TokenPair("mock-access-token", "mock-refresh-token", 3600L, 72000L);
        given(tokenService.generateTokenPair(any(), any(), any())).willReturn(tokenPair);

        // 5. 회원 프로필 조회 Mocking
        MemberProfile memberProfile = Mockito.mock(MemberProfile.class);
        given(memberProfileRepository.findMemberProfileByMemberId(anyLong())).willReturn(Optional.of(memberProfile));

        // 6. MemberProfileMapper Mocking
        given(memberProfileMapper.fromEntityToSummary(any(MemberProfile.class))).willReturn(Mockito.mock(MemberProfileDto.Summary.class));

        // When & Then
        try (MockedStatic<JwtProcessor> jwtProcessorMock = mockStatic(JwtProcessor.class)) {
            // Act
            authenticationService.login(loginRequest, request, response);

            // Assert: Mockito.inOrder를 통한 실행 순서 검증
            InOrder inOrder = Mockito.inOrder(authenticationService, jwtKeyHelper, tokenService);

            // (1) 비밀번호 일치 여부 확인
            inOrder.verify(authenticationService).checkIfPasswordMatchesOrThrow(anyString(), anyString());

            // (2) Access Token 만료 시간 생성
            inOrder.verify(jwtKeyHelper).generateAccessTokenExpiration();

            // (3) 토큰 쌍(Access, Refresh) 생성
            inOrder.verify(tokenService).generateTokenPair(any(), any(), any());

            // (4) Response Header에 JWT 추가 (정적 메서드 검증)
            jwtProcessorMock.verify(() -> JwtProcessor.addJwtAtResponseHeader(eq("mock-access-token"), eq(response)));
        }
    }
}
