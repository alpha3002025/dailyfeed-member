package click.dailyfeed.member.domain.member.projection;

import click.dailyfeed.code.domain.member.member.type.data.CountryCode;
import click.dailyfeed.code.domain.member.member.type.data.GenderType;
import click.dailyfeed.code.domain.member.member.type.data.PrivacyLevel;
import click.dailyfeed.code.domain.member.member.type.data.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface MemberProfileProjection {
    Long id();
    Long memberId();
    String memberName();
    String handle();
    String displayName();
    String bio();
    String location();
    String websiteUrl();
    LocalDate birthDate();
    GenderType gender();
    String timezone();
    String languageCode ();
    CountryCode countryCode();
    VerificationStatus verificationStatus();
    PrivacyLevel privacyLevel();
    Integer profileCompletionScore();
    Boolean isActive();
    String avatarUrl();
    String coverUrl();
    LocalDateTime createdAt();
    LocalDateTime updatedAt();
}
