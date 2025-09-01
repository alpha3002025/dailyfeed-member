package click.dailyfeed.member.domain.follow.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.security.CustomUserDetails;
import click.dailyfeed.member.domain.follow.service.FollowService;
import click.dailyfeed.member.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController("/api/members/follows")
public class FollowController {
    private final FollowService followService;
    private final MemberService memberService;

    @PostMapping("/")
    public DailyfeedServerResponse<Boolean> follow(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                   @RequestBody FollowDto.FollowRequest followRequest) {
        Long myId = customUserDetails.getMemberEntity().getId();
        followService.follow(followRequest.getMemberIdToFollow(), myId);
        return DailyfeedServerResponse.<Boolean>builder().ok("Y").reason("SUCCESS").statusCode("201").data(Boolean.TRUE).build();
    }

    @DeleteMapping("/")
    public DailyfeedServerResponse<Boolean> unfollow(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                     @RequestBody FollowDto.UnfollowRequest unfollowRequest) {
        Long myId = customUserDetails.getMemberEntity().getId();
        followService.unfollow(unfollowRequest.getMemberIdToUnfollow(), myId);
        return DailyfeedServerResponse.<Boolean>builder().ok("Y").reason("DELETE_SUCCESS").statusCode("204").data(Boolean.TRUE).build();
    }

    // 나의 팔로우 목록
    // todo (페이징처리가 필요하다)
    @GetMapping("/")
    public DailyfeedServerResponse<FollowDto.Follow> getMyFollow(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        // 팔로워, 팔로잉 목록
        // 팔로워 수, 팔로잉 수
        Long myId = customUserDetails.getMemberEntity().getId();
        FollowDto.Follow follow = followService.getMyFollow(myId);
        return DailyfeedServerResponse.<FollowDto.Follow>builder().ok("Y").reason("SUCCESS").statusCode("200").data(follow).build();
    }

    // 특정 멤버의 팔로워,팔로잉
    // todo (페이징처리가 필요하다)
    @GetMapping("/member/{memberId}")
    public DailyfeedServerResponse<FollowDto.Follow> getMemberFollow(@PathVariable Long memberId){
        // 팔로워, 팔로잉 목록
        // 팔로워 수, 팔로잉 수
        FollowDto.Follow follow = followService.getMemberFollow(memberId);
        return DailyfeedServerResponse.<FollowDto.Follow>builder().ok("Y").reason("SUCCESS").statusCode("200").data(follow).build();
    }
    
    @GetMapping("/latest/posts")
    public DailyfeedPageResponse<FollowDto.LatestPost> getLatestPosts(
            @RequestHeader("Authorization") String token,
            HttpServletResponse response,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable
    ) {
        return null;
    }
}
