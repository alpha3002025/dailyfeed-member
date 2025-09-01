package click.dailyfeed.member.config.security;

import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService{
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) throw new UsernameNotFoundException("email is Empty");

        Member member = memberRepository.findByEmail(email);
        if (member == null)
            throw new UsernameNotFoundException("member " + email + " is Empty");

        return new CustomUserDetails(member);
    }
}
