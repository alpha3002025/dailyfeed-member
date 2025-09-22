package click.dailyfeed.member.domain.authentication.mapper;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.member.domain.authentication.dto.AuthenticationDto;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.entity.MemberProfileImage;
import click.dailyfeed.code.domain.member.member.type.data.ImageType;
import click.dailyfeed.code.domain.member.member.type.data.ImageCategory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 일단 Plain 하게 작성함 (시간이 없어서)
@Component
public class AuthenticationMapper {
    public Member newMember(AuthenticationDto.SignupRequest signupRequest, PasswordEncoder passwordEncoder, String roles){
        Member member = Member.newMember()
                .name(signupRequest.getMemberName())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .roles(roles)
                .build();

        MemberProfile memberProfile = MemberProfile.builder()
                .member(member)
                .memberName(signupRequest.getMemberName())
                .handle(signupRequest.getHandle())
                .displayName(signupRequest.getDisplayName())
                .bio(signupRequest.getBio())
                .location(signupRequest.getLocation())
                .websiteUrl(signupRequest.getWebsiteUrl())
                .birthDate(signupRequest.getBirthDate())
                .gender(signupRequest.getGender())
                .timezone(signupRequest.getTimezone())
                .languageCode(signupRequest.getLanguageCode())
                .countryCode(signupRequest.getCountryCode())
                .verificationStatus(signupRequest.getVerificationStatus())
                .privacyLevel(signupRequest.getPrivacyLevel())
                .profileCompletionScore(signupRequest.getProfileCompletionScore())
                .isActive(signupRequest.getIsActive())
                .build();

        // Member와 MemberProfile 연관관계 설정
        member.updateMemberProfile(memberProfile);

        // 아바타 이미지가 제공된 경우 추가
        if (signupRequest.getAvatarUrl() != null && !signupRequest.getAvatarUrl().trim().isEmpty()) {
            MemberProfileImage avatarImage = MemberProfileImage.builder()
                    .memberProfile(memberProfile)
                    .imageType(ImageType.AVATAR)
                    .imageCategory(ImageCategory.ORIGINAL)
                    .imageUrl(signupRequest.getAvatarUrl())
                    .isPrimary(true)
                    .build();
            memberProfile.addProfileImage(avatarImage);
        }

        // 커버 이미지가 제공된 경우 추가
        if (signupRequest.getCoverUrl() != null && !signupRequest.getCoverUrl().trim().isEmpty()) {
            MemberProfileImage coverImage = MemberProfileImage.builder()
                    .memberProfile(memberProfile)
                    .imageType(ImageType.COVER)
                    .imageCategory(ImageCategory.ORIGINAL)
                    .imageUrl(signupRequest.getCoverUrl())
                    .isPrimary(true)
                    .build();
            memberProfile.addProfileImage(coverImage);
        }

        return member;
    }

    public MemberDto.Member fromMemberEntityToMemberDto(Member member){
        return MemberDto.Member.builder()
                .id(member.getId())
                .name(member.getName())
                .build();
    }
}
