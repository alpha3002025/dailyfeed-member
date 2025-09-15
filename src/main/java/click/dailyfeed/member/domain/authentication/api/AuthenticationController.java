package click.dailyfeed.member.domain.authentication.api;

import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/signup")
    public AuthenticationDto.SignupResponse signup(
            @Valid @RequestBody AuthenticationDto.SignupRequest signupRequest
    ) {
        log.info("Received signup request - memberName: {}, countryCode: {}, email: {}",
                signupRequest.getMemberName(), signupRequest.getCountryCode(), signupRequest.getEmail());
        return authenticationService.signup(signupRequest);
    }

    @PostMapping("/login")
    public AuthenticationDto.LoginResponse login(
            @Valid @RequestBody AuthenticationDto.LoginRequest loginRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authenticationService.login(loginRequest, httpServletResponse);
    }

    @PostMapping("/logout")
    public AuthenticationDto.LogoutResponse logout(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        return authenticationService.logout(request,response);
    }

}
