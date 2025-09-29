package click.dailyfeed.member.config.security.userdetails;

import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberProfileRepository;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService{
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) throw new UsernameNotFoundException("email is Empty");

        Member member = memberRepository.findFirstByEmailFetchJoin(email)
                .orElseThrow(() -> new MemberNotFoundException());

        return new CustomUserDetails(member);
    }
}
