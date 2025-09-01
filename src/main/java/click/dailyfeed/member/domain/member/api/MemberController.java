package click.dailyfeed.member.domain.member.api;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.global.web.response.DailyfeedServerResponse;
import click.dailyfeed.member.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/members")
@RestController
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/")
    public DailyfeedServerResponse<MemberDto.Member> getMemberByToken(
            @RequestHeader("Authorization") String token,
            HttpServletResponse response
    ) {
        MemberDto.Member member = memberService.findMemberByToken(token);
        memberService.checkAndRefreshHeader(token, response);
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
