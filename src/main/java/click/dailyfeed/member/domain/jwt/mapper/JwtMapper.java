package click.dailyfeed.member.domain.jwt.mapper;

import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.member.entity.Member;

import java.util.Date;

public class JwtMapper {
    public static JwtDto.UserDetails ofUserDetails(Long id, String email, String password, Date expiration){
        return JwtDto.UserDetails.builder()
                .id(id)
                .email(email)
                .password(password)
                // .expiration(expiration) TODO 제거 예정
                .build();
    }

    /**
     * Member 엔티티로부터 직접 UserDetails 생성
     */
    public static JwtDto.UserDetails fromMember(Member member) {
        return JwtDto.UserDetails.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .build();
    }
}
