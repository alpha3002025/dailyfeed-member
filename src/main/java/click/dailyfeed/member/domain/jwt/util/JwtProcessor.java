package click.dailyfeed.member.domain.jwt.util;

import click.dailyfeed.code.global.jwt.exception.*;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.mapper.JwtMapper;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Key;
import java.util.Date;

public class JwtProcessor {

    // 정규식 패턴을 static final로 선언하여 재사용
    private static final java.util.regex.Pattern KID_PATTERN =
            java.util.regex.Pattern.compile("\"kid\"\\s*:\\s*\"([^\"]+)\"");

    public static String generateToken(Key key, String keyId, JwtDto.UserDetails userDetails){
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setSubject(userDetails.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + 864000000))
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

    public static String extractKeyIdOrThrow(String token) {
        try {
            // JWT는 "header.payload.signature" 형태
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new InvalidTokenException("Invalid JWT Token");
            }

            // Header 부분만 디코딩 (첫 번째 부분)
            String headerJson = new String(
                    java.util.Base64.getUrlDecoder().decode(tokenParts[0])
            );

            // 미리 컴파일된 패턴 사용으로 성능 최적화
            if (headerJson.contains("\"kid\"")) {
                java.util.regex.Matcher matcher = KID_PATTERN.matcher(headerJson);

                if (matcher.find()) {
                    return matcher.group(1);
                }
            }

            throw new TokenMissingClaimsException();

        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }


    public static JwtDto.UserDetails degenerateToken(Key key, String token){
        JwtParser jwtParser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build();

        if(!JwtProcessor.checkContainsBearer(token))
            throw new InvalidTokenException("Invalid JWT Token");

        // jws
        Jws<Claims> jws = getJwsOrThrow(jwtParser, token.replace("Bearer ", ""));

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

    public static Boolean checkIfExpired(Date expiration){
        return expiration.before(new Date());
    }
}
