package click.dailyfeed.member.domain.jwt.mapper;

import click.dailyfeed.member.domain.jwt.dto.JwtDto;

import java.util.Date;

public class JwtMapper {
    public static JwtDto.UserDetails ofUserDetails(Long id, String password, Date expiration){
        return JwtDto.UserDetails.builder()
                .id(id)
                .password(password)
                .expiration(expiration)
                .build();
    }
}
