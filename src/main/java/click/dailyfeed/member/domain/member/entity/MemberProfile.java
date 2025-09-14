package click.dailyfeed.member.domain.member.entity;

import click.dailyfeed.code.domain.member.member.type.data.*;
import click.dailyfeed.code.domain.member.member.validator.MemberProfileValidation;
import click.dailyfeed.member.domain.base.BaseTimeEntity;
import click.dailyfeed.member.domain.member.converter.CountryCodeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@Table(name = "member_profiles", schema = "dailyfeed",
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_handle", columnList = "handle"),
                @Index(name = "idx_country_lang", columnList = "country_code, language_code"),
                @Index(name = "idx_verification", columnList = "verification_status"),
                @Index(name = "idx_updated_at", columnList = "updated_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(staticName = "ofAll")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"profileImages"}) // 순환참조 방지
public class MemberProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Pattern(regexp = MemberProfileValidation.HandleValidation.PATTERN)
    @Column(name = "handle", nullable = false, length = 50, unique = true)
    private String handle;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30)
    private GenderType gender;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "language_code", length = 10)
    @Builder.Default
    private String languageCode = "en";

    @Column(name = "country_code", columnDefinition = "CHAR(2)")
    @Convert(converter = CountryCodeConverter.class)
    private CountryCode countryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_level", length = 20)
    @Builder.Default
    private PrivacyLevel privacyLevel = PrivacyLevel.PUBLIC;

    @Column(name = "profile_completion_score", columnDefinition = "TINYINT")
    @Builder.Default
    private Integer profileCompletionScore = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // 연관관계
    @OneToMany(mappedBy = "memberProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberProfileImage> profileImages = new ArrayList<>();

    // 기본 이미지 URL 상수
    public static final String DEFAULT_AVATAR_URL = "https://cdn-icons-png.flaticon.com/512/10302/10302971.png";
    public static final String DEFAULT_COVER_URL = "https://cdn-icons-png.flaticon.com/512/10302/10302971.png";

    // 편의 메서드
    public void addProfileImage(MemberProfileImage image) {
        profileImages.add(image);
        image.updateMemberProfile(this);
    }

    public void removeProfileImage(MemberProfileImage image) {
        profileImages.remove(image);
        image.updateMemberProfile(null);
    }

    public Optional<String> findPrimaryImageUrl(ImageType imageType) {
        return profileImages.stream()
                .filter(image -> image.getImageType().equals(imageType))
                .filter(MemberProfileImage::getIsPrimary)
                .findFirst()
                .map(MemberProfileImage::getEffectiveImageUrl);
    }

    public String getAvatarUrl(){
        return findPrimaryImageUrl(ImageType.AVATAR).orElse(DEFAULT_AVATAR_URL);
    }

    public String getCoverUrl(){
        return findPrimaryImageUrl(ImageType.COVER).orElse(DEFAULT_COVER_URL);
    }


    public List<String> getAllAvatarUrls(){
        if(profileImages == null || profileImages.isEmpty()){
            return List.of(DEFAULT_AVATAR_URL);
        }

        List<String> avatarUrls = profileImages.stream()
                .filter(image -> ImageType.AVATAR.equals(image.getImageType()))
                .map(MemberProfileImage::getEffectiveImageUrl)
                .toList();

        return avatarUrls.isEmpty() ? List.of(DEFAULT_AVATAR_URL) : avatarUrls;
    }

    /**
     * 이미지가 있는지 확인
     */
    public boolean hasAvatarImage() {
        if (profileImages == null || profileImages.isEmpty()) {
            return false;
        }
        return profileImages.stream()
                .anyMatch(img -> "AVATAR".equals(img.getImageType().name()));
    }

    public boolean hasCoverImage() {
        if (profileImages == null || profileImages.isEmpty()) {
            return false;
        }
        return profileImages.stream()
                .anyMatch(img -> "COVER".equals(img.getImageType().name()));
    }
}
