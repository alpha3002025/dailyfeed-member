package click.dailyfeed.member.domain.authentication.mapper;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.member.entity.Member;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 일단 Plain 하게 작성함 (시간이 없어서)
@Component
public class AuthenticationMapper {
    public Member newMember(AuthenticationDto.SignupRequest signupRequest, PasswordEncoder passwordEncoder, String roles){
        return Member.newMember()
                .name(signupRequest.getMemberName())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .roles(roles)
                .build();
    }

    public MemberDto.Member fromMemberEntityToMemberDto(Member member){
        return MemberDto.Member.builder()
                .id(member.getId())
                .name(member.getName())
                .build();
    }
}
