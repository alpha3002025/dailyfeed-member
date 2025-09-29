package click.dailyfeed.member.domain.member.service;

import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.code.domain.member.member.dto.MemberProfileDto;
import click.dailyfeed.code.domain.member.member.exception.MemberHandleAlreadyExistsException;
import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.feign.domain.image.ImageFeignHelper;
import click.dailyfeed.member.domain.follow.repository.jpa.FollowRepository;
import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.mapper.MemberProfileMapper;
import click.dailyfeed.member.domain.member.repository.jpa.MemberProfileRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberProfileRepository memberProfileRepository;
    private final MemberProfileMapper memberProfileMapper;
    private final FollowRepository followRepository;
    private final ImageFeignHelper imageFeignHelper;

    public MemberProfileDto.MemberProfile updateMemberProfile(
            MemberDto.Member requestedMember, MemberProfileDto.UpdateRequest updateRequest, String token, HttpServletResponse httpResponse) {
        /// 존재하는 회원인지 체크
        MemberProfile memberProfile = memberProfileRepository
                .findMemberProfileByMemberId(requestedMember.getId())
                .orElseThrow(() -> new MemberNotFoundException());

        /// update
        MemberProfile updatedMemberProfile = memberProfileMapper.updateMember(memberProfile, updateRequest);

        /// followers, following 카운트
        Long followersCount = followRepository.countFollowersByMemberId(requestedMember.getId());
        Long followingsCount = followRepository.countFollowingByMemberId(requestedMember.getId());

        /// 회원의 기존 이미지 삭제 요청 → image-svc
        // TODO SEASON 2 근데... 이거 카프카로 분리하는게 맞긴해보인다. 트랜잭션 내에 존재할 필요가 없다.
        if (!updateRequest.getPreviousAvatarUrl().isEmpty()) {
            deletePreviousAvatarImage(updateRequest, token, httpResponse);
        }

        /// mapper 변환
        return memberProfileMapper.fromEntity(updatedMemberProfile, followersCount, followingsCount);
    }

    private void deletePreviousAvatarImage(MemberProfileDto.UpdateRequest updateRequest, String token, HttpServletResponse response) {
        List<String> previousAvatarUrls = updateRequest.getPreviousAvatarUrl();
        MemberProfileDto.ImageDeleteBulkRequest bulkRequest = MemberProfileDto.ImageDeleteBulkRequest.builder().imageUrls(previousAvatarUrls).build();
        imageFeignHelper.deleteImages(bulkRequest, token);
    }

    public String updateMemberProfileHandle(MemberDto.Member requestedMember, MemberProfileDto.HandleChangeRequest handleChangeRequest) {
        /// 존재하는 회원인지 체크
        MemberProfile memberProfile = memberProfileRepository
                .findMemberProfileByMemberId(requestedMember.getId())
                .orElseThrow(() -> new MemberNotFoundException());

        /// 핸들이 존재하는지 체크
        Optional<MemberProfile> handleResult = memberProfileRepository.findByHandle(handleChangeRequest.getNewHandle());
        if(handleResult.isPresent()) {
            throw new MemberHandleAlreadyExistsException();
        }

        /// update
        memberProfile.updateHandle(handleChangeRequest.getNewHandle());
        return memberProfile.getHandle();
    }
}
