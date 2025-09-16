package click.dailyfeed.member.domain.jwt.util;

import click.dailyfeed.code.global.jwt.exception.*;
import click.dailyfeed.code.global.jwt.predicate.JwtExpiredPredicate;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Key;
import java.util.Date;

public class JwtProcessor {


    public static String generateToken(Key key, String keyId, JwtDto.UserDetails userDetails){
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setSubject(userDetails.getEmail())
                .setExpiration(userDetails.getExpiration())
                .claim("id", userDetails.getId())
                .claim("email", userDetails.getEmail())
                .claim("password", userDetails.getPassword())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String generateToken(Key key, JwtDto.UserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + 864000000))
                .claim("id", userDetails.getId())
                .claim("email", userDetails.getEmail())
                .claim("password", userDetails.getPassword())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /// JWT 의 header 내의 key id(=kid) 추출
    public static String extractKeyIdOrThrow(String token) {
        try {
            // 토큰 정리
            if (token == null) {
                throw new InvalidTokenException("Token is null");
            }

            token = token.trim();

            // Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            // 모든 공백 문자 제거
            token = token.replaceAll("\\s", "");

            // 토큰 구조 확인
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                throw new InvalidTokenException("Invalid JWT Token");
            }

            // Base64 URL-safe 디코딩
            byte[] headerBytes = java.util.Base64.getUrlDecoder().decode(chunks[0]);
            String headerJson = new String(headerBytes, java.nio.charset.StandardCharsets.UTF_8);

            // JSON에서 kid 추출
            if (headerJson.contains("\"kid\"")) {
                int kidStart = headerJson.indexOf("\"kid\":\"") + 7;
                int kidEnd = headerJson.indexOf("\"", kidStart);
                if (kidStart > 6 && kidEnd > kidStart) {
                    return headerJson.substring(kidStart, kidEnd);
                }
            }

            throw new TokenMissingClaimsException();

        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid JWT Token format");
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }


    public static JwtDto.UserDetails degenerateToken(Key key, String token){
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build();

        // jws
        Jws<Claims> jws = getJwsOrThrow(jwtParser, token);

        // id
        Long id = getIdOrThrow(jws);
        // email
        String email = getEmailOrThrow(jws);
        // password
        String password = getPasswordOrThrow(jws);
        // expiration
        Date expiration = getExpirationDateOrThrow(jws);

        return JwtMapper.ofUserDetails(id, email, password, expiration);
    }

    public static Jws<Claims> getJwsOrThrow(JwtParser jwtParser, String token){
        Jws<Claims> jws = jwtParser.parseClaimsJws(token);

        if(jws == null)
            throw new InvalidTokenException();
        if(jws.getBody() == null)
            throw new TokenPayloadEmptyException();

        return jws;
    }

    public static Long getIdOrThrow(Jws<Claims> jws){
        if(jws.getBody().get("id") == null) {
            throw new TokenPayloadEmptyException();
        }

        Long id = String.valueOf(jws.getBody().get("id")).isBlank() ? null : Long.valueOf(String.valueOf(jws.getBody().get("id")));
        if(id == null) {
            throw new TokenMissingClaimsException();
        }
        return id;
    }

    public static String getEmailOrThrow(Jws<Claims> jws){
        if(jws.getBody().get("email") == null) {
            throw new TokenMissingClaimsException();
        }
        String email = String.valueOf(jws.getBody().get("email")).isBlank() ? null : String.valueOf(jws.getBody().get("email"));
        if(email == null) {
            throw new TokenMissingClaimsException();
        }
        return email;
    }

    public static String getPasswordOrThrow(Jws<Claims> jws){
        if(jws.getBody().get("password") == null) {
            throw new TokenMissingClaimsException();
        }
        String password = String.valueOf(jws.getBody().get("password")).isBlank() ? null : String.valueOf(jws.getBody().get("password"));
        if(password == null) {
            throw new TokenMissingClaimsException();
        }
        return password;
    }

    public static Date getExpirationDateOrThrow(Jws<Claims> jws){
        Date expiration = jws.getBody().getExpiration();
        if(expiration == null) {
            throw new TokenMissingExpirationException();
        }
        return expiration;
    }

    public static void addJwtAtResponseHeader(String jwt, HttpServletResponse response) {
        response.addHeader("Authorization", "Bearer " + jwt);
    }

    public static Boolean checkContainsBearer(String valueString) {
        return valueString != null && valueString.startsWith("Bearer ");
    }

    public static JwtExpiredPredicate checkIfExpired(Date expiration){
        if(expiration.before(new Date())) return JwtExpiredPredicate.EXPIRED;
        return JwtExpiredPredicate.NOT_EXPIRED;
    }

    public static String getJwtFromHeaderOrThrow(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            throw new BearerTokenMissingException();
        }

        return authHeader.replace("Bearer ", "");
    }
}
