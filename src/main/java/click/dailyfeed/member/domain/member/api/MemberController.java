package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/members")
@RestController
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/")
    public DailyfeedServerResponse<MemberDto.Member> getMemberByToken(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response
    ) {
        // SecurityContext에서 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // JWT 필터에서 설정한 이메일
        
        MemberDto.Member member = memberService.findMemberDtoByEmail(email);
        
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

    @GetMapping("/{id}")
    public DailyfeedServerResponse<MemberDto.Member> getMember(@PathVariable("id") Long id){
        MemberDto.Member member = memberService.findMemberById(id);
        return DailyfeedServerResponse.<MemberDto.Member>builder()
                .data(member).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    @GetMapping("/list")
    public DailyfeedServerResponse<List<MemberDto.Member>> getMemberList(
            @RequestParam("ids") List<Long> memberIds
    ){
        List<MemberDto.Member> members = memberService.findMembersByIds(memberIds);
        return DailyfeedServerResponse.<List<MemberDto.Member>>builder()
                .data(members).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }

    @PostMapping("/list")
    public DailyfeedServerResponse<List<MemberDto.Member>> getMembersBulkPost(
            @RequestBody MemberDto.MembersBulkRequest request
    ){
        List<MemberDto.Member> members = memberService.findMembersByIds(request.getIds());
        return DailyfeedServerResponse.<List<MemberDto.Member>>builder()
                .data(members).ok("Y").statusCode("200").reason("SUCCESS")
                .build();
    }
}
