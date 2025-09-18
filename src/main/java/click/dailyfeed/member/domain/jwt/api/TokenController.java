package click.dailyfeed.member.domain.jwt.api;

import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/token")
@RestController
@RequiredArgsConstructor
public class TokenController {
    private final JwtKeyHelper jwtKeyHelper;

    @GetMapping("/refresh")
    public DailyfeedServerResponse<Boolean> refreshKey(
            @RequestHeader("Authorization") String authorizationHeader
    ){
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }
}
