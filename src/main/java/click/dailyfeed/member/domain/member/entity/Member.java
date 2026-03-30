package click.dailyfeed.member.domain.member.entity;

import click.dailyfeed.member.domain.base.BaseTimeEntity;
import click.dailyfeed.member.domain.follow.entity.Follow;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "ofAll")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String password; // encrypted password

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private MemberProfile memberProfile;

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followers = new ArrayList<>();

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followings = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberEmail> memberEmails = new ArrayList<>();

    @Builder(builderMethodName = "newMember")
    public Member(String password, String roles) {
        this.password = password;
        this.roles = roles;
    }

    private String roles;

    public List<String> getRoleList(){
        if (roles != null && roles.length() > 0){
            return Arrays.asList(roles.split(","));
        }
        else{
            return new ArrayList<>();
        }
    }

    public void addEmail(MemberEmail email) {
        this.memberEmails.add(email);
        email.couplingMember(this);
    }

    public void addFollowing(Follow follow) {
        this.followings.add(follow);
    }

    public void addFollower(Follow follow) {
        this.followers.add(follow);
    }

    public void removeFollowing(Follow follow){
        this.followings.remove(follow);
    }

    public void removeFollower(Follow follow){
        this.followers.remove(follow);
    }

    public void follow(Member memberToFollow, Follow follow) {
//        this.followings.add(follow);
//        memberToFollow.followers.add(follow);
        addFollowing(follow);
        memberToFollow.addFollower(follow);

        ///  Follow 에 대한 처리 역시 수행 (위 코드 들만 사용할 경우 실제 주인데이터인 Follow 는 바뀌지 않을수 있음)
        follow.couplingFollower(this);
        follow.couplingFollowing(memberToFollow);
    }

    public void unfollow(Member memberToUnfollow, Follow follow) {
//        this.followings.remove(follow);
//        memberToUnfollow.followers.remove(follow);
        removeFollowing(follow);
        memberToUnfollow.removeFollower(follow);
    }

    public Boolean isFollowing(Member memberToFollow) {
        return this.followings.stream().anyMatch(follow -> follow.getFollowing().equals(memberToFollow));
    }

    public Long getFollowingCount(){
        return Long.parseLong(String.valueOf(getFollowings().size()));
    }

    public Long getFollowerCount(){
        return Long.parseLong(String.valueOf(getFollowers().size()));
    }
    
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateMemberProfile(MemberProfile memberProfile) {
        this.memberProfile = memberProfile;
        /// MemberProfile 내에도 Member 설정
        if (memberProfile != null && memberProfile.getMember() != this){
            memberProfile.couplingMember(this);
        }
    }
}
