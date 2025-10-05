package click.dailyfeed.member.domain.follow.service;

import click.dailyfeed.code.domain.member.follow.exception.FollowLimitExceedsException;
import click.dailyfeed.code.domain.member.follow.exception.FollowRelationshipAlreadyExistsException;
import click.dailyfeed.code.domain.member.follow.exception.FollowRelationshipNotFoundException;
import click.dailyfeed.code.domain.member.follow.predicate.FollowingPredicate;
import click.dailyfeed.code.domain.member.follow.properties.FollowProperties;
import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.code.global.web.page.DailyfeedPage;
import click.dailyfeed.code.global.web.page.DailyfeedPageable;
import click.dailyfeed.code.global.web.page.DailyfeedScrollPage;
import click.dailyfeed.member.domain.follow.document.FollowingDocument;
import click.dailyfeed.member.domain.follow.entity.Follow;
import click.dailyfeed.member.domain.follow.mapper.FollowMapper;
import click.dailyfeed.member.domain.follow.repository.jpa.FollowRepository;
import click.dailyfeed.member.domain.follow.repository.mongo.FollowingMongoRepository;
import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.jpa.MemberProfileRepository;
import click.dailyfeed.member.domain.member.repository.jpa.MemberRepository;
import click.dailyfeed.pagination.converter.DailyfeedPageableConverter;
import click.dailyfeed.pagination.mapper.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final FollowingMongoRepository followingMongoRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileMapper memberProfileMapper;

    private final DailyfeedPageableConverter dailyfeedPageableConverter;
    private final PageMapper pageMapper;
    private final FollowMapper followMapper;

    /**
     * "followerId 가 memberToFollowId 를 follow 한다"
     * @param followerId 팔로우 요청한 멤버 id
     * @param memberToFollowId 팔로우 하려는 멤버 id
     * @return
     */
    public Boolean follow(Long followerId, Long memberToFollowId) {
        Member memberToFollow = getMemberOrThrow(memberToFollowId);
        Member follower = getMemberOrThrow(followerId);

        checkFollowingCountExceedsOrThrow(follower);

        FollowingPredicate followingPredicate = checkMemberFollowingSomeone(follower, memberToFollow);
        if(followingPredicate == FollowingPredicate.FOLLOWING) {
            throw new FollowRelationshipAlreadyExistsException();
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(memberToFollow)
                .build();

        follower.follow(memberToFollow, follow);

        followingMongoRepository.save(followMapper.newFollowDocument(followerId, memberToFollowId));
        return true;
    }

    /**
     * "followerId 가 memberToUnfollowId 를 unfollow 한다"
     * @param followerId 언팔로우 요청한 멤버 id
     * @param memberToUnfollowId 언팔로우 하려는 멤버 id
     * @return
     */
    public Boolean unfollow(Long followerId, Long memberToUnfollowId) {
        Member memberToUnfollow = getMemberOrThrow(memberToUnfollowId);
        Member follower = getMemberOrThrow(followerId);
        Follow follow = getFollowRelationshipOrThrowIfNotFound(memberToUnfollow, follower);

        follower.unfollow(memberToUnfollow, follow);
        deleteFollowingDocument(followerId, memberToUnfollowId);

        return Boolean.TRUE;
    }

    public void deleteFollowingDocument(Long followerId, Long memberToUnfollowId) {
        Optional<FollowingDocument> documentResult = followingMongoRepository.findByFromIdAndToId(followerId, memberToUnfollowId);
        if(documentResult.isPresent()) {
            followingMongoRepository.delete(documentResult.get());
        }
    }

    public Member getMemberOrThrow(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    public Follow getFollowRelationshipOrThrowIfNotFound(Member follower, Member following) {
        return followRepository
                .findByFollowerAndFollowing(follower, following)
                .orElseThrow(FollowRelationshipNotFoundException::new);
    }

    public void checkFollowingCountExceedsOrThrow(Member member) {
        Long currentCnt = followRepository.countMemberFollowing(member.getId());
        if(currentCnt >= FollowProperties.FOLLOWING_MAX_LIMIT){
            throw new FollowLimitExceedsException();
        }
    }

    public FollowingPredicate checkMemberFollowingSomeone(Member member, Member someone){
        Optional<Follow> check = followRepository.findMemberFollowingSomeone(member.getId(), someone.getId());
        if(check.isPresent()){
            return FollowingPredicate.FOLLOWING;
        }
        return FollowingPredicate.NOT_FOLLOWING;
    }

    @Transactional(readOnly = true)
    public DailyfeedPage<MemberProfileDto.Summary> getRecommendNewbie(MemberDto.Member requestedMember, Pageable pageable) {
        Page<MemberProfile> memberProfiles = memberProfileRepository.findWithImagesOrderByCreatedAtWithPaging(requestedMember.getId(), pageable);
        List<MemberProfileDto.Summary> content = memberProfiles.stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return pageMapper.fromJpaPageToDailyfeedPage(memberProfiles, content);
    }

    @Transactional(readOnly = true)
    public DailyfeedScrollPage<MemberProfileDto.Summary> getRecommendNewbieMore(MemberDto.Member requestedMember, DailyfeedPageable dailyfeedPageable) {
        /// pageable
        Pageable pageable = dailyfeedPageableConverter.convert(dailyfeedPageable);

        /// follwoing
        Page<MemberProfile> memberProfiles = memberProfileRepository.findWithImagesOrderByCreatedAtWithPaging(requestedMember.getId(), pageable);

        /// 변환
        List<MemberProfileDto.Summary> content = memberProfiles.stream()
                .map(memberProfileMapper::fromEntityToSummary)
                .toList();

        return pageMapper.fromJpaPageToDailyfeedScrollPage(memberProfiles, content);
    }
}
