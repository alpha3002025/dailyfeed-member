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
        System.out.println("=== MemberController getMemberByToken called ===");
        
        // SecurityContext에서 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // JWT 필터에서 설정한 이메일
        
        System.out.println("Authenticated user email: " + email);
        
        MemberDto.Member member = memberService.findMemberDtoByEmail(email);
        
        System.out.println("Member found: " + member.getName() + " (" + member.getEmail() + ")");
        
        // checkAndRefreshHeader는 일단 주석 처리 (JWT 재검증으로 에러 발생 가능성)
        // if (token != null) {
        //     memberService.checkAndRefreshHeader(token, response);
        // }
        
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
