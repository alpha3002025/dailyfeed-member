package click.dailyfeed.member.domain.follow;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.response.DailyfeedScrollResponse;
import click.dailyfeed.member.domain.follow.repository.FollowRepository;
import click.dailyfeed.member.domain.follow.service.FollowRedisService;
import click.dailyfeed.member.domain.follow.service.FollowService;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import click.dailyfeed.member.fixtures.MemberDataSet001;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ActiveProfiles({"local-test"})
@SpringBootTest
public class FollowUnFollowFollowersPageFollowingsPageTest {
    @Autowired
    private MemberDataSet001 memberDataSet001;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FollowRedisService followRedisService;

    @Autowired
    private FollowService followService;

    @Autowired
    private FollowRepository followRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    private final static Logger logger = LoggerFactory.getLogger(FollowUnFollowFollowersPageFollowingsPageTest.class);

    // 캐시 비우기 (테스트 시에는 캐시를 비워야 함)
    @BeforeEach
    public void clearCache() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    @Transactional
    @Test
    public void TEST__멤버리스트_조회(){
        memberDataSet001.init();

        List<Member> members = memberRepository.findAll();
        members.forEach(member -> {
            System.out.println(member.getName());
        });
    }

    @Transactional
    @Test
    public void TEST_팔로우_테스트(){
        memberDataSet001.init();
        List<Member> members = memberRepository.findAll();

        Member member = members.get(0);

        followService.follow(members.get(1).getId(), member.getId());
        followService.follow(members.get(2).getId(), member.getId());
        followService.follow(members.get(3).getId(), member.getId());

        List<String> a_followingMemberNames = member.getFollowings()
                .stream()
                .map(follow -> follow.getFollower().getName())
                .toList();

        List<String> a_followerMemberNames = member.getFollowers()
                .stream()
                .map(follow -> follow.getFollower().getName())
                .toList();

        logger.info("a_followingMemberNames: {}", a_followingMemberNames.toString());
        logger.info("a_followerMemberNames: {}", a_followerMemberNames.toString());

        Assertions.assertThat(member.getFollowers()).hasSize(3);
        Assertions.assertThat(member.getFollowings()).hasSize(0);
        Assertions.assertThat(a_followingMemberNames).isEmpty();
        Assertions.assertThat(a_followerMemberNames).contains("B", "C", "D");
    }

    @Transactional
    @Test
    public void 팔로우_팔로워_리스트_검색(){
        memberDataSet001.init();
        List<Member> members = memberRepository.findAll();

        Member member = members.get(0);

        followService.follow(members.get(1).getId(), member.getId());
        followService.follow(members.get(2).getId(), member.getId());
        followService.follow(members.get(3).getId(), member.getId());

        // (팔로잉/팔로우) 멤버 'A' 가 팔로잉하는 멤버 수 = 0
        List<MemberProfileDto.Summary> a_followingMembers = followRedisService.getFollowingMembers(member.getId());
        Assertions.assertThat(a_followingMembers).hasSize(0);
        // (팔로워) 멤버 'A' 를 팔로우하는 멤버 수 = 3
        Page<Long> memberA_followMembers = followRepository.findFollowersIdByMemberId(member.getId(), PageRequest.of(0, 10));
        Assertions.assertThat(memberA_followMembers.getContent()).hasSize(3);

        // (팔로잉/팔로우) 멤버 'B' 가 팔로잉하는 멤버 수 = 1
        Long memberBId = members.get(1).getId();
        List<MemberProfileDto.Summary> b_followingMembers = followRedisService.getFollowingMembers(memberBId);
        Assertions.assertThat(b_followingMembers).hasSize(1);
        // (팔로워) 멤버 'B' 를 팔로우하는 멤버 수 = 0
        Page<Long> memberB_followingMembers = followRepository.findFollowersIdByMemberId(memberBId, PageRequest.of(0, 10));
        Assertions.assertThat(memberB_followingMembers.getContent()).hasSize(0);

        // (팔로잉/팔로우) 멤버 'C' 가 팔로잉하는 멤버 수 = 1
        Long memberCId = members.get(2).getId();
        List<MemberProfileDto.Summary> c_followingMembers = followRedisService.getFollowingMembers(memberCId);
        Assertions.assertThat(c_followingMembers).hasSize(1);
        // (팔로워) 멤버 'C' 를 팔로우하는 멤버 수 = 0
        Page<Long> memberC_followingMembers = followRepository.findFollowersIdByMemberId(memberCId, PageRequest.of(0, 10));
        Assertions.assertThat(memberC_followingMembers.getContent()).hasSize(0);
    }

    @Transactional
    @Test
    public void 언팔로우_테스트(){
        memberDataSet001.init();
        List<Member> members = memberRepository.findAll();

        Member member = members.get(0);

        followService.follow(members.get(1).getId(), member.getId());
        followService.follow(members.get(2).getId(), member.getId());
        followService.follow(members.get(3).getId(), member.getId());

        // (팔로잉/팔로우) 멤버 'A' 가 팔로잉하는 멤버 수 = 0
        List<MemberProfileDto.Summary> a_followingMembers = followRedisService.getFollowingMembers(member.getId());
        Assertions.assertThat(a_followingMembers).hasSize(0);
        // (팔로워) 멤버 'A' 를 팔로우하는 멤버 수 = 3
        Page<Long> memberA_followMembers = followRepository.findFollowersIdByMemberId(member.getId(), PageRequest.of(0, 10));
        Assertions.assertThat(memberA_followMembers.getContent()).hasSize(3);

        // 멤버 'B' 가 멤버 'A' 를 언팔로우
        followService.unfollow(members.get(1).getId(), member.getId());
        // case 1
        List<MemberProfileDto.Summary> case1_followingMembers = followRedisService.getFollowingMembers(member.getId());
        Assertions.assertThat(case1_followingMembers).hasSize(0);
        // (팔로워) 멤버 'A' 를 팔로우하는 멤버 수 = 2
        Page<Long> case1_followMembers = followRepository.findFollowersIdByMemberId(member.getId(), PageRequest.of(0, 10));
        Assertions.assertThat(case1_followMembers.getContent()).hasSize(2);

        // 멤버 'C' 가 멤버 'A' 를 언팔로우
        followService.unfollow(members.get(2).getId(), member.getId());
        // case 1
        List<MemberProfileDto.Summary> case2_followingMembers = followRedisService.getFollowingMembers(member.getId());
        Assertions.assertThat(case2_followingMembers).hasSize(0);
        // (팔로워) 멤버 'A' 를 팔로우하는 멤버 수 = 1
        Page<Long> case2_followMembers = followRepository.findFollowersIdByMemberId(member.getId(), PageRequest.of(0, 10));
        Assertions.assertThat(case2_followMembers.getContent()).hasSize(1);

        // 멤버 'D' 가 멤버 'A' 를 언팔로우
        followService.unfollow(members.get(3).getId(), member.getId());
        // case 1
        List<MemberProfileDto.Summary> case3_followingMembers = followRedisService.getFollowingMembers(member.getId());
        Assertions.assertThat(case3_followingMembers).hasSize(0);
        // (팔로워) 멤버 'A' 를 팔로우하는 멤버 수 = 0
        Page<Long> case3_followMembers = followRepository.findFollowersIdByMemberId(member.getId(), PageRequest.of(0, 10));
        Assertions.assertThat(case3_followMembers.getContent()).hasSize(0);

        // 멤버 'A' 의 팔로잉,팔로워
        FollowDto.FollowScrollPage scrollResponse = followRedisService.getMemberFollow(member.getId(), DailyfeedPageable.of(0, 10));
        Assertions.assertThat(scrollResponse.getFollowings().getContent().size()).isEqualTo(0);
        Assertions.assertThat(scrollResponse.getFollowers().getContent().size()).isEqualTo(0);
    }

}
