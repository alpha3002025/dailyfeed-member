package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.code.ResponseSuccessCode;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.web.annotation.InternalAuthenticatedMember;
import click.dailyfeed.member.domain.follow.service.FollowRedisService;
import click.dailyfeed.member.domain.member.redis.MemberRedisService;
import click.dailyfeed.member.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/members")
@RestController
public class MemberController {
    private final MemberService memberService;
    private final MemberRedisService memberRedisService;
    private final FollowRedisService followRedisService;

    /// member 존재 조회
    @GetMapping({""})
    public DailyfeedServerResponse<MemberDto.Member> getMemberByToken(
            @InternalAuthenticatedMember MemberDto.Member member
    ) {
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    /// member profile 관련 영역
    //  로그인한 사용자의 프로필
    @GetMapping("/profile")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> getMemberProfile(
            @InternalAuthenticatedMember MemberDto.Member requestedMember
    ){
        MemberProfileDto.MemberProfile member = memberRedisService.findMemberProfileById(requestedMember.getId());
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/profile/@{memberHandle}")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> getMemberProfileByHandle(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberHandle") String memberHandle
    ){
        MemberProfileDto.MemberProfile member = memberRedisService.findMemberProfileByHandle(memberHandle);
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 다른 사람의 프로필 조회
    @GetMapping("/profile/{memberId}")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> getMemberProfileByMemberId(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberId") Long memberId
    ){
        MemberProfileDto.MemberProfile member = memberRedisService.findMemberProfileById(memberId);
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 프로필 업데이트 요청
    @PutMapping("/profile")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> updateMemberProfile(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @RequestHeader("Authorization") String token,
            HttpServletResponse httpResponse,
            @RequestBody MemberProfileDto.UpdateRequest updateRequest
    ){
        log.info("updateMemberProfile called - memberId: {}, token exists: {}, updateRequest: {}",
                requestedMember.getId(), token != null, updateRequest);
        MemberProfileDto.MemberProfile member = memberService.updateMemberProfile(requestedMember, updateRequest, token, httpResponse);
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 프로필 handle 업데이트 요청
    @PutMapping("/profile/handle")
    public DailyfeedServerResponse<String> updateMemberProfileHandle(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @Valid @RequestBody MemberProfileDto.HandleChangeRequest handleChangeRequest
    ){
        String result = memberService.updateMemberProfileHandle(requestedMember, handleChangeRequest);
        return DailyfeedServerResponse.<String>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // TODO SEASON 2 : 이메일 변경, 이메일 인증

    /// 로그인한 사용자의 Summary
    @GetMapping("/summary")
    public DailyfeedServerResponse<MemberProfileDto.Summary> getMemberSummary(
            @InternalAuthenticatedMember MemberDto.Member requestedMember
    ){
        MemberProfileDto.Summary member = memberRedisService.findMemberSummaryById(requestedMember.getId());
        return DailyfeedServerResponse.<MemberProfileDto.Summary>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }


    /// 나의 팔로워/팔로우 목록
    @GetMapping("/followers-followings")
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMyFollow(
            @InternalAuthenticatedMember MemberDto.Member requestMember,
            DailyfeedPageable dailyfeedPageable
    ){
        FollowDto.FollowScrollPage result = followRedisService.getMemberFollow(requestMember.getId(), dailyfeedPageable);
        return DailyfeedScrollResponse.<FollowDto.FollowScrollPage>builder()
                .data(result)
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
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowingsMore(requestedMember.getId(), dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/followers/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowersMore(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowersMore(requestedMember.getId(), dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    ///  특정 용도
    ///  특정 id 리스트에 대한 멤버 정보 조회 (타임라인)
    @PostMapping("/query/in")
    public DailyfeedServerResponse<List<MemberProfileDto.Summary>> getMembersQueryIn(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @Valid @RequestBody MemberDto.MembersIdsQuery query
    ){
        List<MemberProfileDto.Summary> members = memberRedisService.findMembersByIdsIn(query.getIds());
        return DailyfeedServerResponse.<List<MemberProfileDto.Summary>>builder()
                .data(members)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/query/followings")
    public DailyfeedServerResponse<List<MemberProfileDto.Summary>> getMemberQueryFollowings(
            @InternalAuthenticatedMember MemberDto.Member requestedMember
    ){
        List<MemberProfileDto.Summary> followingMembers = followRedisService.getFollowingMembers(requestedMember.getId());
        return DailyfeedServerResponse.<List<MemberProfileDto.Summary>>builder()
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .data(followingMembers)
                .build();
    }

    /// {memberId} /// 빠른 조회가 필요할 경우, 특정 Member Id 에 대한 단건 조회만 할 경우 (아이덴티티 조회 전용)
    @GetMapping("/{id}")
    public DailyfeedServerResponse<MemberDto.Member> getMember(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("id") Long id
    ){
        MemberDto.Member member = memberRedisService.getMemberOrThrow(id);
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 다른 사람의 프로필 조회
    @GetMapping("/{memberId}/profile")
    public DailyfeedServerResponse<MemberProfileDto.MemberProfile> getAnotherMemberProfile(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberId") Long memberId
    ){
        MemberProfileDto.MemberProfile member = memberRedisService.findMemberProfileById(memberId);
        return DailyfeedServerResponse.<MemberProfileDto.MemberProfile>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 다른 사람의 프로필 Summary 조회
    @GetMapping("/{memberId}/summary")
    public DailyfeedServerResponse<MemberProfileDto.Summary> getAnotherMemberSummary(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberId") Long memberId
    ){
        MemberProfileDto.Summary member = memberRedisService.findMemberSummaryById(memberId);
        return DailyfeedServerResponse.<MemberProfileDto.Summary>builder()
                .data(member)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    // 특정 멤버의 팔로워,팔로잉
    @GetMapping("/{memberId}/followers-followings")
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMemberFollow(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        FollowDto.FollowScrollPage result = followRedisService.getMemberFollow(memberId, dailyfeedPageable);
        return DailyfeedScrollResponse.<FollowDto.FollowScrollPage>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/{memberId}/followers/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowMore(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowersMore(memberId, dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }

    @GetMapping("/{memberId}/followings/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<MemberProfileDto.Summary>> getMemberFollowingMore(
            @InternalAuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        DailyfeedScrollPage<MemberProfileDto.Summary> result = followRedisService.getMemberFollowingsMore(memberId, dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
        return DailyfeedScrollResponse.<DailyfeedScrollPage<MemberProfileDto.Summary>>builder()
                .data(result)
                .status(HttpStatus.OK.value())
                .result(ResponseSuccessCode.SUCCESS)
                .build();
    }
}
