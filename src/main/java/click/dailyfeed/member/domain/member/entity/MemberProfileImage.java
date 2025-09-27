package click.dailyfeed.member.domain.member.entity;

import click.dailyfeed.code.domain.member.member.type.data.ImageCategory;
import click.dailyfeed.code.domain.member.member.type.data.ImageType;
import click.dailyfeed.member.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "member_profile_images", schema = "dailyfeed",
        indexes = {
                @Index(name = "idx_profile_id", columnList = "profile_id"),
                @Index(name = "idx_profile_type_category", columnList = "profile_id, image_type, image_category"),
                @Index(name = "idx_primary", columnList = "profile_id, is_primary")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(staticName = "ofAll")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = {"memberProfile"}) // 순환참조 방지
public class MemberProfileImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    @EqualsAndHashCode.Include
    private Long imageId;

    @Column(name = "profile_id", nullable = false, insertable = false, updatable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 20)
    @Builder.Default
    private ImageType imageType = ImageType.AVATAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_category", nullable = false, length = 20)
    private ImageCategory imageCategory;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "width", columnDefinition = "smallint unsigned")
    private Integer width;

    @Column(name = "height", columnDefinition = "smallint unsigned")
    private Integer height;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "cdn_url", length = 1000)
    private String cdnUrl;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "upload_source", length = 50)
    private String uploadSource;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private MemberProfile memberProfile;

    // 편의 메서드
    public String getEffectiveImageUrl() {
        return cdnUrl != null ? cdnUrl : imageUrl;
    }

    public boolean isAvatar() {
        return ImageType.AVATAR.equals(imageType);
    }

    public boolean isCover() {
        return ImageType.COVER.equals(imageType);
    }

    public boolean isGallery() {
        return ImageType.GALLERY.equals(imageType);
    }

    public void updateMemberProfile(MemberProfile memberProfile) {
        this.memberProfile = memberProfile;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
