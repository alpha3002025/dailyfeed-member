package click.dailyfeed.member.domain.authentication.mapper;

import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.member.entity.Member;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 일단 Plain 하게 작성함 (시간이 없어서)
@Component
public class AuthenticationMapper {

    public AuthenticationDto.LoginResponse ofLoginSuccess(){
        return AuthenticationDto.LoginResponse.builder()
                .ok("Y").statusCode("200").reason("LOGIN_SUCCESS")
                .build();
    }

    public AuthenticationDto.LoginResponse ofLoginFail(String reason){
        return AuthenticationDto.LoginResponse.builder()
                .ok("N").statusCode("401").reason(reason)
                .build();
    }

    public AuthenticationDto.SignupResponse ofSignupSuccess(){
        return AuthenticationDto.SignupResponse.builder()
                .ok("Y").statusCode("201").reason("SIGNUP_SUCCESS")
                .build();
    }

    public AuthenticationDto.SignupResponse ofSignupFail(String reason){
        return AuthenticationDto.SignupResponse.builder()
                .ok("N").statusCode("403").reason(reason)
                .build();
    }

    public Member newMember(AuthenticationDto.SignupRequest signupRequest, PasswordEncoder passwordEncoder, String roles){
        return Member.newMember()
                .name(signupRequest.getName())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .roles(roles)
                .build();
    }

    public AuthenticationDto.LogoutResponse ofLogoutSuccessResponse(String reason){
        return AuthenticationDto.LogoutResponse.builder()
                .ok("Y").statusCode("200").reason("LOGOUT_SUCCESS")
                .build();
    }
}
