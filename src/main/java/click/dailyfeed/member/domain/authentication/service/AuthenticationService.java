package click.dailyfeed.member.domain.authentication.service;

import click.dailyfeed.code.domain.member.member.exception.MemberAlreadyExistsException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.domain.member.member.predicate.MemberExistsPredicate;
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

        // 1. JwtProcessor 를 이용해 token 을 만든다 (키 ID 포함)
        // 토큰 만료 시간을 현재 시간 + 1시간으로 설정
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1시간 = 3600000ms
        JwtDto.UserDetails userDetails = JwtMapper.ofUserDetails(member.getId(), member.getEmail(), member.getPassword(), expirationDate);
        String token = jwtKeyHelper.generateToken(userDetails); // JwtKeyHelper의 메서드 사용 (키 ID 포함)

        // 2. response Header 에 token 을 심는다.
        JwtProcessor.addJwtAtResponseHeader(token, response);

        return authenticationMapper.ofLoginSuccess();
    }

    public AuthenticationDto.SignupResponse signup(AuthenticationDto.SignupRequest signupRequest) {
        if (MemberExistsPredicate.EXISTS.equals(checkIfMemberAlreadyExists(signupRequest))) {
            throw new MemberAlreadyExistsException();
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
            throw new MemberNotFoundException();
        }
        return member;
    }

    public void checkIfPasswordMatchesOrThrow(String requestPassword, String encryptedPassword) {
        if (!passwordEncoder.matches(requestPassword, encryptedPassword)) {
            throw new UsernameNotFoundException("PASSWORD_MISMATCH");
        }
    }

    @Transactional(readOnly = true)
    public MemberExistsPredicate checkIfMemberAlreadyExists(AuthenticationDto.SignupRequest signupRequest) {
        if (memberRepository.findByEmail(signupRequest.getEmail()) == null) {
            return MemberExistsPredicate.NOT_EXISTS;
        }
        return MemberExistsPredicate.EXISTS;
    }
}
