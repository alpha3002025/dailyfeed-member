package click.dailyfeed.member.domain.jwt.mapper;

import click.dailyfeed.member.domain.jwt.entity.JwtKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

@Component
public class JwtKeyPlainMapper {
    /**
     * 키를 Java Key 객체로 변환합니다.
     */
    public Key convertToKey(JwtKey jwtKey) {
        byte[] decodedKey = Base64.getDecoder().decode(jwtKey.getSecretKey());
        return Keys.hmacShaKeyFor(decodedKey);
    }
}
