package click.dailyfeed.member.domain.jwt.mapper;

import click.dailyfeed.member.domain.jwt.dto.JwtDto;

import java.util.Date;

public class JwtMapper {
    public static JwtDto.UserDetails ofUserDetails(Long id, Date expiration){
        return JwtDto.UserDetails.builder()
                .id(id)
                .expiration(expiration)
                .build();
    }
}
