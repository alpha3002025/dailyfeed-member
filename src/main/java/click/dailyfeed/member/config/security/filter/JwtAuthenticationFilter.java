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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;

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

        if (header != null && header.startsWith("Bearer ")) {
            if(JwtProcessor.checkContainsBearer(header)){
                JwtDto.UserDetails userDetails = JwtProcessor.degenerateToken(key, header);

                if(JwtProcessor.checkIfExpired(userDetails.getExpiration())){
                    throw new IllegalArgumentException("로그인 기한이 만료되었습니다. 다시 로그인 해주세요.");
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails.getEmail(), userDetails.getPassword());
                authenticationManager.authenticate(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
