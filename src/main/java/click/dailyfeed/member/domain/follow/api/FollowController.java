package click.dailyfeed.member.domain.follow.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedPageResponse;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.web.annotation.InternalAuthenticatedMember;
import click.dailyfeed.member.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/members/follow")
@RequiredArgsConstructor
@RestController
public class FollowController {
    private final FollowService followService;

    @PostMapping("")
    public DailyfeedServerResponse<Boolean> follow(@InternalAuthenticatedMember MemberDto.Member member,
                                                   @RequestBody FollowDto.FollowRequest followRequest) {
        Long myId = member.getId();
        followService.follow(myId, followRequest.getMemberIdToFollow());
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.CREATED.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(Boolean.TRUE)
                .build();
    }

    @DeleteMapping("")
    public DailyfeedServerResponse<Boolean> unfollow(@InternalAuthenticatedMember MemberDto.Member member,
                                                     @RequestBody FollowDto.UnfollowRequest unfollowRequest) {
        Long myId = member.getId();
        followService.unfollow(myId, unfollowRequest.getMemberIdToUnfollow());
        return DailyfeedServerResponse.<Boolean>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(Boolean.TRUE)
                .build();
    }

    /// 사용자 추천
    @GetMapping("/recommend/newbie")
    public DailyfeedPageResponse<MemberProfileDto.Summary> getRecommendNewbie(
            @InternalAuthenticatedMember MemberDto.Member member,
            @PageableDefault(
                    size = 10,
                    page = 0,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ){
        DailyfeedPage<MemberProfileDto.Summary> content = followService.getRecommendNewbie(member, pageable);
        return DailyfeedPageResponse.<MemberProfileDto.Summary>builder()
                .data(content)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    /// 사용자 추천 scroll
    @GetMapping("/recommend/newbie/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getRecommendNewbieMore(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followService.getRecommendNewbieMore(requestedMember, dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }
}
