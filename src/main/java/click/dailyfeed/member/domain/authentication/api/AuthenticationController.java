package click.dailyfeed.member.domain.authentication.api;

import click.dailyfeed.code.domain.authentication.code.AuthenticationExceptionCode;
import click.dailyfeed.code.domain.authentication.exception.AuthenticationException;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.service.AuthenticationService;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/authentication")
@RequiredArgsConstructor
@RestController
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final JwtKeyHelper jwtKeyHelper;
    private final TokenService tokenService;

    @PostMapping("/signup")
    public DailyfeedServerResponse<MemberDto.Member> signup(
            @Valid @RequestBody AuthenticationDto.SignupRequest signupRequest
    ) {
        log.info("Received signup request - memberName: {}, countryCode: {}, email: {}",
                signupRequest.getMemberName(), signupRequest.getCountryCode(), signupRequest.getEmail());
        return authenticationService.signup(signupRequest);
    }

    @PostMapping("/login")
    public DailyfeedServerResponse<Boolean> login(
            @Valid @RequestBody AuthenticationDto.LoginRequest loginRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authenticationService.login(loginRequest, httpServletRequest, httpServletResponse);
    }

    @PostMapping("/logout")
    public DailyfeedServerResponse<Boolean> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        return authenticationService.logout(request,response);
    }

    @PostMapping("/refresh")
    public DailyfeedServerResponse<AuthenticationDto.TokenRefreshResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        return authenticationService.refreshToken(request, response);
    }

    @PostMapping("/logout-all")
    public DailyfeedServerResponse<Boolean> logoutAllDevices(
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                Long memberId = jwtKeyHelper.extractMemberId(accessToken);
                tokenService.logoutAllDevices(memberId);
                return DailyfeedServerResponse.<Boolean>builder()
                        .content(Boolean.TRUE)
                        .status(HttpStatus.OK.value())
                        .result(ResponseSuccessCode.SUCCESS)
                        .build();
            }

            return DailyfeedServerResponse.<Boolean>builder()
                    .content(Boolean.TRUE)
                    .status(HttpStatus.OK.value())
                    .result(ResponseSuccessCode.SUCCESS)
                    .build();

        } catch (Exception e) {
            log.error("Logout all devices error: {}", e.getMessage());
            throw new AuthenticationException(AuthenticationExceptionCode.LOGOUT_FAIL_UNAUTHORIZED);
        }
    }

}
