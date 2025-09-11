package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.follow.service.FollowService;
import click.dailyfeed.member.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
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
    private final MemberService memberService;
    private final FollowService followService;

    @GetMapping("/")
    public DailyfeedServerResponse<MemberDto.Member> getMemberByToken(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response
    ) {
        // SecurityContext에서 인증된 사용자 정보 가져오기
        MemberDto.Member member = memberService.findMemberByToken(token);
        
        // JWT 키 갱신 필요 여부 체크 (예외 발생해도 응답은 계속 진행)
        if (token != null) {
            try {
                memberService.checkAndRefreshHeader(token, response);
            } catch (Exception e) {
                // 에러가 발생해도 계속 진행 (사용자 정보는 정상 반환)
            }
        }
        
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    ///  로그인한 사용자의 프로필
    // ✅ TODO 임시 ( 팔로우/팔로잉 카운트 및 기타 부가정보들 가져오는 API)
    // 변경될 경우 dailyfeed-feign 반영 필요
    @GetMapping("/profile")
    public DailyfeedServerResponse<MemberDto.MemberProfile> getMemberProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response
    ){
        MemberDto.MemberProfile member = memberService.findMemberProfileByToken(token);
        return DailyfeedServerResponse.<MemberDto.MemberProfile>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    // 나의 팔로우 목록
    // todo (페이징처리가 필요하다) 페이징, token 처리 AOP 적용 🫡
    @GetMapping("/followers-followings")
    public DailyfeedServerResponse<FollowDto.FollowPage> getMyFollow(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ){
        FollowDto.FollowPage follow = followService.getMyFollow(pageable, token, response);
        return DailyfeedServerResponse.<FollowDto.FollowPage>builder().ok("Y").reason("SUCCESS").statusCode("200").data(follow).build();
    }

    @GetMapping("/followings")
    public DailyfeedServerResponse<List<FollowDto.Following>> getMemberFollowings(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @PathVariable Long memberId){
        List<FollowDto.Following> followingMembers = followService.getFollowingMembers(memberId, token, response);
        return DailyfeedServerResponse.<List<FollowDto.Following>>builder().ok("Y").reason("SUCCESS").statusCode("200").data(followingMembers).build();
    }

    ///  특정 용도
    ///  특정 id 리스트에 대한 멤버 정보 조회
    @PostMapping("/query/in")
    public DailyfeedServerResponse<List<MemberDto.Member>> getMembersQueryIn(
            @Valid @RequestBody MemberDto.MembersIdsQuery query
    ){
        List<MemberDto.Member> members = memberService.findMembersByIds(query.getIds());
        return DailyfeedServerResponse.<List<MemberDto.Member>>builder()
                .data(members).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    /// {memberId}
    //  ✅ TODO (삭제 or 통합조회 API 검토) : member 하나만 달랑 들고오는 교과서적인 REST API 는 없다.
    //     member 처럼 다양한 특성을 가진 케이스의 경우 사실상 모든걸 때려박아서 가져오는 API 는 불가능하다고 보임
    @Deprecated
    @GetMapping("/{id}")
    public DailyfeedServerResponse<MemberDto.Member> getMember(@PathVariable("id") Long id){
        MemberDto.Member member = memberService.findMemberById(id);
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    // 다른 사람의 프로필 조회
    @GetMapping("/{memberId}/profile")
    public DailyfeedServerResponse<MemberDto.MemberProfile> getAnotherMemberProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response,
            @PathVariable("memberId") Long memberId
    ){
        MemberDto.MemberProfile member = memberService.findAnotherMemberProfile(memberId, token, response);
        return DailyfeedServerResponse.<MemberDto.MemberProfile>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    // 특정 멤버의 팔로워,팔로잉
    // ✅ todo (페이징처리가 필요하다) 페이징, token 처리 AOP 적용 🫡
    @GetMapping("/{memberId}/followers-followings")
    public DailyfeedServerResponse<FollowDto.FollowPage> getMemberFollow(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @PathVariable Long memberId){
        // (todo) 팔로워 수, 팔로잉 수 (캐싱 → count 쿼리)
        FollowDto.FollowPage follow = followService.getMemberFollow(memberId, pageable, token, response);
        return DailyfeedServerResponse.<FollowDto.FollowPage>builder().ok("Y").reason("SUCCESS").statusCode("200").data(follow).build();
    }

}
