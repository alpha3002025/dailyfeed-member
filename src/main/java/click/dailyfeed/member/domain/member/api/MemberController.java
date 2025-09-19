package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.web.annotation.AuthenticatedMember;
import click.dailyfeed.member.domain.follow.service.FollowRedisService;
import click.dailyfeed.member.domain.member.redis.MemberRedisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/members")
@RestController
public class MemberController {
    private final MemberRedisService memberRedisService;
    private final FollowRedisService followRedisService;

    @GetMapping("/")
    public DailyfeedServerResponse<MemberDto.Member> getMemberByToken(
            @AuthenticatedMember MemberDto.Member member
    ) {
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .content(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    ///  로그인한 사용자의 프로필
    @GetMapping("/profile")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> getMemberProfile(
            @AuthenticatedMember MemberDto.Member requestedMember
    ){
        MemberProfileDto.MemberProfile member = memberRedisService.findMemberProfileById(requestedMember.getId());
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .content(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    /// 나의 팔로워/팔로우 목록
    @GetMapping("/followers-followings")
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMyFollow(
            @AuthenticatedMember MemberDto.Member requestMember,
            DailyfeedPageable dailyfeedPageable
    ){
        FollowDto.FollowScrollPage result = followRedisService.getMemberFollow(requestMember.getId(), dailyfeedPageable);
        return DailyfeedScrollResponse.<FollowDto.FollowScrollPage>builder()
                .content(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

//    @GetMapping("/followings") // 처음 받아온 /followers-followings → /more
//    public DailyfeedServerResponse<List<FollowDto.Following>> getMemberFollowings(
//            @AuthenticatedMember MemberDto.Member requestedMember
//    ){
//        return null;
//    }

    @GetMapping("/followings/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowingsMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){

        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowingsMore(requestedMember.getId(), dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .content(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/followers/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowersMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowersMore(requestedMember.getId(), dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .content(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    ///  특정 용도
    ///  특정 id 리스트에 대한 멤버 정보 조회 (타임라인)
    @PostMapping("/query/in")
    public DailyfeedServerResponse<List<MemberProfileDto.Summary>> getMembersQueryIn(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @Valid @RequestBody MemberDto.MembersIdsQuery query
    ){
        List<MemberProfileDto.Summary> members = memberRedisService.findMembersByIdsIn(query.getIds());
        return DailyfeedServerResponse.<List<MemberProfileDto.Summary>>builder()
                .content(members)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/query/followings")
    public DailyfeedServerResponse<List<MemberProfileDto.Summary>> getMemberQueryFollowings(
            @AuthenticatedMember MemberDto.Member requestedMember
    ){
        List<MemberProfileDto.Summary> followingMembers = followRedisService.getFollowingMembers(requestedMember.getId());
        return DailyfeedServerResponse.<List<MemberProfileDto.Summary>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .content(followingMembers)
                .build();
    }

    /// {memberId} /// 빠른 조회가 필요할 경우, 특정 Member Id 에 대한 단건 조회만 할 경우 (아이덴티티 조회 전용)
    @GetMapping("/{id}")
    public DailyfeedServerResponse<MemberDto.Member> getMember(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("id") Long id
    ){
        MemberDto.Member member = memberRedisService.getMemberOrThrow(id);
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .content(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 다른 사람의 프로필 조회
    @GetMapping("/{memberId}/profile")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> getAnotherMemberProfile(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberId") Long memberId
    ){
        MemberProfileDto.MemberProfile member = memberRedisService.findMemberProfileById(memberId);
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .content(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 다른 사람의 프로필 Summary 조회
    @GetMapping("/{memberId}/summary")
    public DailyfeedServerResponse<MemberProfileDto.Summary> getAnotherMemberSummary(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberId") Long memberId
    ){
        MemberProfileDto.Summary member = memberRedisService.findMemberSummaryById(memberId);
        return DailyfeedServerResponse.<MemberProfileDto.Summary>builder()
                .content(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 특정 멤버의 팔로워,팔로잉
    @GetMapping("/{memberId}/followers-followings")
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMemberFollow(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        FollowDto.FollowScrollPage result = followRedisService.getMemberFollow(memberId, dailyfeedPageable);
        return DailyfeedScrollResponse.<FollowDto.FollowScrollPage>builder()
                .content(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/{memberId}/followers/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowersMore(memberId, dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .content(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/{memberId}/followings/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowingMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowingsMore(memberId, dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .content(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }
}
