package click.dailyfeed.member.domain.follow.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.security.userdetails.CustomUserDetails;
import click.dailyfeed.member.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/members/follow")
@RequiredArgsConstructor
@RestController
public class FollowController {
    private final FollowService followService;

    @PostMapping("/")
    public DailyfeedServerResponse<Boolean> follow(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                   @RequestBody FollowDto.FollowRequest followRequest) {
        Long myId = customUserDetails.getMemberEntity().getId();
        followService.follow(myId, followRequest.getMemberIdToFollow());
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }

    @DeleteMapping("/")
    public DailyfeedServerResponse<Boolean> unfollow(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                     @RequestBody FollowDto.UnfollowRequest unfollowRequest) {
        Long myId = customUserDetails.getMemberEntity().getId();
        followService.unfollow(myId, unfollowRequest.getMemberIdToUnfollow());
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(Boolean.TRUE)
                .build();
    }
}
