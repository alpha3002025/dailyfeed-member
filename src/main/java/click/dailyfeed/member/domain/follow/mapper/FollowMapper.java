package click.dailyfeed.member.domain.follow.mapper;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.member.domain.member.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FollowMapper {
    FollowMapper INSTANCE = Mappers.getMapper(FollowMapper.class);

    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "name", source = "member.name")
    @Mapping(target = "email", source = "member.email")
    FollowDto.Follower toFollower(Member member);

    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "name", source = "member.name")
    @Mapping(target = "email", source = "member.email")
    FollowDto.Following toFollowing(Member member);
}
