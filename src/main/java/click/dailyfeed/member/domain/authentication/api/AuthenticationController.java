package click.dailyfeed.member.domain.authentication.api;

import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.authentication.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/authentication")
@RequiredArgsConstructor
@RestController
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @GetMapping("/signup")
    public AuthenticationDto.SignupResponse signup(@RequestBody AuthenticationDto.SignupRequest signupRequest) {
        return authenticationService.signup(signupRequest);
    }

    @GetMapping("/login")
    public AuthenticationDto.LoginResponse login(
            @RequestBody AuthenticationDto.LoginRequest loginRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authenticationService.login(loginRequest, httpServletResponse);
    }

    @GetMapping("/logout")
    public AuthenticationDto.LogoutResponse logout(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        return authenticationService.logout(request,response);
    }

}
