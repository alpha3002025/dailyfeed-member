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
                .memberId(memberProfile.getMember().getId())
                .memberName(memberProfile.getMemberName())
                .handle(memberProfile.getHandle())
                .displayName(memberProfile.getDisplayName())
                .bio(memberProfile.getBio())
                .location(memberProfile.getLocation())
                .websiteUrl(memberProfile.getWebsiteUrl())
                .birthDate(memberProfile.getBirthDate())
                .gender(memberProfile.getGender())
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
                .memberId(memberProfile.getMember().getId())
                .memberName(memberProfile.getMemberName())
                .memberHandle(memberProfile.getHandle())
                .displayName(memberProfile.getDisplayName())
                .verificationStatus(memberProfile.getVerificationStatus())
                .avatarUrl(memberProfile.getAvatarUrl())
                .updatedAt(memberProfile.getUpdatedAt())
                .build();
    }

    default MemberProfile updateMember(MemberProfile memberProfile, MemberProfileDto.UpdateRequest updateRequest){
        if (updateRequest.getMemberName() != null) memberProfile.updateMemberName(updateRequest.getMemberName());
        if (updateRequest.getDisplayName() != null) memberProfile.updateDisplayName(updateRequest.getDisplayName());
        if (updateRequest.getBio() != null) memberProfile.updateBio(updateRequest.getBio());
        if (updateRequest.getLocation() != null) memberProfile.updateLocation(updateRequest.getLocation());
        if (updateRequest.getWebsiteUrl() != null) memberProfile.updateWebsiteUrl(updateRequest.getWebsiteUrl());
        if (updateRequest.getBirthDate() != null) memberProfile.updateBirthDate(updateRequest.getBirthDate());
        if (updateRequest.getGender() != null) memberProfile.updateGender(updateRequest.getGender());
        if (updateRequest.getLanguageCode() != null) memberProfile.updateLanguageCode(updateRequest.getLanguageCode());
        if (updateRequest.getCountryCode() != null) memberProfile.updateCountryCode(updateRequest.getCountryCode());
        if (updateRequest.getPrivacyLevel() != null) memberProfile.updatePrivacyLevel(updateRequest.getPrivacyLevel());

        if (updateRequest.getAvatarUrl() != null) {
            memberProfile.updateAvatarUrl(updateRequest.getAvatarUrl());
        }
        return memberProfile;
    }
}
