package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.web.AuthenticatedMember;
import click.dailyfeed.member.domain.follow.service.FollowRedisService;
import click.dailyfeed.member.domain.follow.service.FollowService;
import click.dailyfeed.member.domain.member.redis.MemberRedisService;
import click.dailyfeed.member.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/members")
@RestController
public class MemberController {
    private final FollowService followService;
    private final MemberRedisService memberRedisService;
    private final FollowRedisService followRedisService;

    @GetMapping("/")
    public DailyfeedServerResponse<MemberDto.Member> getMemberByToken(
            @AuthenticatedMember MemberDto.Member member
    ) {
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    ///  로그인한 사용자의 프로필
    // ✅ TODO 임시 ( 팔로우/팔로잉 카운트 및 기타 부가정보들 가져오는 API)
    @GetMapping("/profile")
    public DailyfeedServerResponse<MemberDto.MemberProfile> getMemberProfile(
            @AuthenticatedMember MemberDto.Member requestedMember
    ){
        MemberDto.MemberProfile member = memberRedisService.findMemberProfileById(requestedMember.getId());
        return DailyfeedServerResponse.<MemberDto.MemberProfile>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    // 나의 팔로우 목록
    // todo (페이징처리가 필요하다) 페이징 🫡
    @GetMapping("/followers-followings")
    public DailyfeedServerResponse<FollowDto.FollowPage> getMyFollow(
            @AuthenticatedMember MemberDto.Member requestMember,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ){
        FollowDto.FollowPage follow = followRedisService.getMemberFollow(requestMember.getId(), pageable);
        return DailyfeedServerResponse.<FollowDto.FollowPage>builder()
                .ok("Y").reason("SUCCESS").statusCode("200").data(follow)
                .build();
    }

    @GetMapping("/followings")
    public DailyfeedServerResponse<List<FollowDto.Following>> getMemberFollowings(
            @AuthenticatedMember MemberDto.Member requestedMember
    ){
        List<FollowDto.Following> followingMembers = followRedisService.getFollowingMembers(requestedMember.getId());
        return DailyfeedServerResponse.<List<FollowDto.Following>>builder()
                .ok("Y").reason("SUCCESS").statusCode("200").data(followingMembers)
                .build();
    }

    ///  특정 용도
    ///  특정 id 리스트에 대한 멤버 정보 조회 (타임라인)
    @PostMapping("/query/in")
    public DailyfeedServerResponse<List<MemberDto.Member>> getMembersQueryIn(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @Valid @RequestBody MemberDto.MembersIdsQuery query
    ){
        List<MemberDto.Member> members = memberRedisService.findMembersByIds(query.getIds());
        return DailyfeedServerResponse.<List<MemberDto.Member>>builder()
                .data(members).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    /// {memberId}
    //  ✅ TODO (삭제 or 통합조회 API 검토) : member 하나만 달랑 들고오는 교과서적인 REST API 는 없다.
    //     member 처럼 다양한 특성을 가진 케이스의 경우 사실상 모든걸 때려박아서 가져오는 API 는 불가능하다고 보임
    @Deprecated
    @GetMapping("/{id}")
    public DailyfeedServerResponse<MemberDto.Member> getMember(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("id") Long id
    ){
        MemberDto.Member member = memberRedisService.getMemberOrThrow(id);
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member)
                .ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    // 다른 사람의 프로필 조회
    @GetMapping("/{memberId}/profile")
    public DailyfeedServerResponse<MemberDto.MemberProfile> getAnotherMemberProfile(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PathVariable("memberId") Long memberId
    ){
        MemberDto.MemberProfile member = memberRedisService.findMemberProfileById(memberId);
        return DailyfeedServerResponse.<MemberDto.MemberProfile>builder()
                .data(member)
                .ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    // 특정 멤버의 팔로워,팔로잉
    @GetMapping("/{memberId}/followers-followings")
    public DailyfeedServerResponse<FollowDto.FollowPage> getMemberFollow(
            @AuthenticatedMember MemberDto.Member requestedMember,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @PathVariable Long memberId){
        FollowDto.FollowPage follow = followRedisService.getMemberFollow(memberId, pageable);
        return DailyfeedServerResponse.<FollowDto.FollowPage>builder()
                .ok("Y").reason("SUCCESS").statusCode("200")
                .data(follow)
                .build();
    }

}
