package click.dailyfeed.member.config.security.userdetails;

import click.dailyfeed.member.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Getter
public class CustomUserDetails implements UserDetails {
    private final Member memberEntity;

    public CustomUserDetails(Member memberEntity) {
        this.memberEntity = memberEntity;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();

        memberEntity.getRoleList()
                .forEach(role -> {
                    authorities.add(new GrantedAuthority() {
                        @Override
                        public String getAuthority() {
                            return role;
                        }
                    });
                });

        return authorities;
    }

    @Override
    public String getPassword() {
        return memberEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return memberEntity.getMemberEmails().get(0).getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 임시
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 임시
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 임시
    }

    @Override
    public boolean isEnabled() {
        return true; // 임시
    }
}
