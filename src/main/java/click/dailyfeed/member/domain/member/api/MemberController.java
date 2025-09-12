package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.response.DailyfeedScrollPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.config.web.AuthenticatedMember;
import click.dailyfeed.member.domain.follow.service.FollowRedisService;
import click.dailyfeed.member.domain.follow.service.FollowService;
import click.dailyfeed.member.domain.member.redis.MemberRedisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /// 나의 팔로워/팔로우 목록
    @GetMapping("/followers-followings")
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMyFollow(
            @AuthenticatedMember MemberDto.Member requestMember,
            DailyfeedPageable dailyfeedPageable
    ){
        return followRedisService.getMemberFollow(requestMember.getId(), dailyfeedPageable);
    }

//    @GetMapping("/followings") // 처음 받아온 /followers-followings → /more
//    public DailyfeedServerResponse<List<FollowDto.Following>> getMemberFollowings(
//            @AuthenticatedMember MemberDto.Member requestedMember
//    ){
//        return null;
//    }

    @GetMapping("/followings/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<FollowDto.Following>> getMemberFollowingsMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){
        return followRedisService.getMemberFollowingsMore(requestedMember.getId(), dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
    }

    @GetMapping("/followers/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<FollowDto.Follower>> getMemberFollowersMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable
    ){
        return followRedisService.getMemberFollowersMore(requestedMember.getId(), dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
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

    @GetMapping("/query/followings")
    public DailyfeedServerResponse<List<FollowDto.Following>> getMemberQueryFollowings(
            @AuthenticatedMember MemberDto.Member requestedMember
    ){
        List<FollowDto.Following> followingMembers = followRedisService.getFollowingMembers(requestedMember.getId());
        return DailyfeedServerResponse.<List<FollowDto.Following>>builder()
                .ok("Y").reason("SUCCESS").statusCode("200").data(followingMembers)
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
    public DailyfeedScrollResponse<FollowDto.FollowScrollPage> getMemberFollow(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        return followRedisService.getMemberFollow(memberId, dailyfeedPageable);
    }

    @GetMapping("/{memberId}/followers/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<FollowDto.Follower>> getMemberFollowMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        return followRedisService.getMemberFollowersMore(memberId, dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
    }

    @GetMapping("/{memberId}/followings/more")
    public DailyfeedScrollResponse<DailyfeedScrollPage<FollowDto.Following>> getMemberFollowingMore(
            @AuthenticatedMember MemberDto.Member requestedMember,
            DailyfeedPageable dailyfeedPageable,
            @PathVariable Long memberId){
        return followRedisService.getMemberFollowingsMore(memberId, dailyfeedPageable.getPage(), dailyfeedPageable.getSize(), dailyfeedPageable);
    }
}
