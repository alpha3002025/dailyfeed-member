package click.dailyfeed.member.domain.member.mapper;

import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberProfileMapper {
    MemberProfileMapper INSTANCE = Mappers.getMapper(MemberProfileMapper.class);

    default MemberProfileDto.MemberProfile fromEntity(MemberProfile memberProfile, Long followersCount, Long followingsCount) {
        return MemberProfileDto.MemberProfile.builder()
                .id(memberProfile.getId())
                .memberId(memberProfile.getId())
                .memberName(memberProfile.getMemberName())
                .handle(memberProfile.getHandle())
                .displayName(memberProfile.getDisplayName())
                .bio(memberProfile.getBio())
                .location(memberProfile.getLocation())
                .websiteUrl(memberProfile.getWebsiteUrl())
                .birthDate(memberProfile.getBirthDate())
                .gender(memberProfile.getGender())
                .timezone(memberProfile.getTimezone())
                .languageCode(memberProfile.getLanguageCode())
                .countryCode(memberProfile.getCountryCode())
                .verificationStatus(memberProfile.getVerificationStatus())
                .privacyLevel((memberProfile.getPrivacyLevel() == null) ? null : memberProfile.getPrivacyLevel())
                .profileCompletionScore(memberProfile.getProfileCompletionScore())
                .isActive(memberProfile.getIsActive())
                .avatarUrl(memberProfile.getAvatarUrl())
                .coverUrl(memberProfile.getCoverUrl())
                .createdAt(memberProfile.getCreatedAt())
                .updatedAt(memberProfile.getUpdatedAt())
                .followersCount(followersCount)
                .followingsCount(followingsCount)
                .build();
    }

    default MemberProfileDto.Summary fromEntityToSummary(
            MemberProfile memberProfile
    ){
        return MemberProfileDto.Summary.builder()
                .id(memberProfile.getId())
                .memberId(memberProfile.getId())
                .memberName(memberProfile.getMemberName())
                .memberHandle(memberProfile.getHandle())
                .displayName(memberProfile.getDisplayName())
                .verificationStatus(memberProfile.getVerificationStatus())
                .avatarUrl(memberProfile.getAvatarUrl())
                .updatedAt(memberProfile.getUpdatedAt())
                .build();
    }
}
