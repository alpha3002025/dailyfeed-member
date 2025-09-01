package click.dailyfeed.member.domain.authentication.service;

import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.mapper.AuthenticationMapper;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Date;

// TODO : Response 를 Exception -> Enum + ControllerAdvice + Mapper 구조로 전환하려고 하는데, 시간상 현재 버전에서는 모두 Exception 으로만 처리

@RequiredArgsConstructor
@Transactional
@Service
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final AuthenticationMapper authenticationMapper;
    private final JwtKeyHelper jwtKeyHelper;

    @Transactional(readOnly = true)
    public AuthenticationDto.LoginResponse login(AuthenticationDto.LoginRequest loginRequest, HttpServletResponse response) {
        Member member = getMemberOrThrow(loginRequest);
        String encryptedPassword = member.getPassword();

        checkIfPasswordMatchesOrThrow(loginRequest.getPassword(), encryptedPassword);

        // 1. JwtProcessor 를 이용해 token 을 만든다
        Key currentJwtKey = jwtKeyHelper.getCurrentJwtKey();
        JwtDto.UserDetails userDetails = JwtMapper.ofUserDetails(member.getId(), member.getEmail(), member.getPassword(), new Date());
        String token = JwtProcessor.generateToken(currentJwtKey, userDetails);

        // 2. response Header 에 token 을 심는다.
        JwtProcessor.addJwtAtResponseHeader(token, response);

        return authenticationMapper.ofLoginSuccess();
    }

    public AuthenticationDto.SignupResponse signup(AuthenticationDto.SignupRequest signupRequest) {
        if (checkIfMemberAlreadyExists(signupRequest)) {
            return authenticationMapper.ofSignupFail("MEMBER_ALREADY_EXISTS");
        }
        Member newMember = authenticationMapper.newMember(signupRequest, passwordEncoder, "MEMBER");
        Member saved = memberRepository.save(newMember);
        return authenticationMapper.ofSignupSuccess();
    }

    public AuthenticationDto.LogoutResponse logout(HttpServletRequest request, HttpServletResponse response) {
        // Spring Security를 사용한 로그아웃
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            // SecurityContextLogoutHandler를 사용하여 표준 로그아웃 처리
            new SecurityContextLogoutHandler().logout(request, response, authentication);

            // SecurityContext 명시적 클리어
            SecurityContextHolder.clearContext();
        }

        // JWT 토큰 무효화를 위한 Response Header 설정 (선택사항)
        response.setHeader("Authorization", "");
        response.setHeader("Set-Cookie", "JSESSIONID=; Path=/; HttpOnly; Max-Age=0");

        return authenticationMapper.ofLogoutSuccessResponse("LOGOUT_SUCCESS");
    }

    @Transactional(readOnly = true)
    public Member getMemberOrThrow(AuthenticationDto.LoginRequest loginRequest) {
        Member member = memberRepository.findByEmail(loginRequest.getEmail());
        if (member == null) {
            throw new IllegalArgumentException("MEMBER_NOT_FOUND");
        }
        return member;
    }

    public void checkIfPasswordMatchesOrThrow(String requestPassword, String encryptedPassword) {
        if (!passwordEncoder.matches(requestPassword, encryptedPassword)) {
            throw new UsernameNotFoundException("PASSWORD_MISMATCH");
        }
    }

    @Transactional(readOnly = true)
    public Boolean checkIfMemberAlreadyExists(AuthenticationDto.SignupRequest signupRequest) {
        if (memberRepository.findByEmail(signupRequest.getEmail()) == null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
