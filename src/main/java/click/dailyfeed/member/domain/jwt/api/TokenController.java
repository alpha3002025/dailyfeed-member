package click.dailyfeed.member.domain.jwt.api;

import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.jwt.dto.JwtDto;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import click.dailyfeed.member.domain.jwt.util.JwtProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/token")
@RestController
@RequiredArgsConstructor
public class TokenController {
    private final JwtKeyHelper jwtKeyHelper;

    @GetMapping("/refresh")
    public DailyfeedServerResponse<Boolean> refreshKey(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletResponse response
    ){
        try {
            // 1. Authorization 헤더에서 JWT 추출 (Bearer 제거)
            String token = JwtProcessor.getJwtFromHeaderOrThrow(authorizationHeader);

            // 2. JWT 헤더에서 kid (Key ID) 추출
            String keyId = JwtProcessor.extractKeyIdOrThrow(token);

            // 3. 추출한 keyId를 사용하여 JWT를 복호화하고 사용자 정보 추출
            JwtDto.UserDetails userDetails = jwtKeyHelper.readUserDetailsFromToken(keyId, token);

            // 4. 현재 Primary Key를 사용하여 새로운 JWT 생성
            String newToken = jwtKeyHelper.generateToken(userDetails);

            // 5. Response Header에 새로운 JWT 추가
            JwtProcessor.addJwtAtResponseHeader(newToken, response);

            log.info("Token refreshed successfully - Old KeyId: {}, MemberId: {}", keyId, userDetails.getId());

            return DailyfeedServerResponse.<Boolean>builder()
                    .status(HttpStatus.OK.value())
                    .result(ResponseSuccessCode.SUCCESS)
                    .data(Boolean.TRUE)
                    .build();

        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage(), e);
            return DailyfeedServerResponse.<Boolean>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .result(ResponseSuccessCode.FAIL)
                    .data(Boolean.FALSE)
                    .build();
        }
    }
}
