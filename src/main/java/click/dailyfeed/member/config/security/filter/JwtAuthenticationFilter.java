package click.dailyfeed.member.config.security.filter;

import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final Key key;

    public JwtAuthenticationFilter(
            AuthenticationManager authenticationManager,
            Key key
    ) {
        this.authenticationManager = authenticationManager;
        this.key = key;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (header != null && header.startsWith("Bearer ")) {
                if(JwtProcessor.checkContainsBearer(header)){
                    JwtDto.UserDetails userDetails = JwtProcessor.degenerateToken(key, header);

                    if(JwtProcessor.checkIfExpired(userDetails.getExpiration())){
                        throw new IllegalArgumentException("로그인 기한이 만료되었습니다. 다시 로그인 해주세요.");
                    }
                    
                    // JWT 토큰이 이미 검증되었으므로 인증된 상태로 설정
                    // authorities를 포함한 생성자를 사용하면 자동으로 authenticated = true가 됨
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails.getEmail(), 
                        null, // 패스워드는 null로 설정 (이미 JWT로 인증됨)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER")) // MEMBER 권한
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // 예외가 발생해도 필터 체인을 계속 진행 (인증 실패로 처리)
        }

        filterChain.doFilter(request, response);
    }
}
