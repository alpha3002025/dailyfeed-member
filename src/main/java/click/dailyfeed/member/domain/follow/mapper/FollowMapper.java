package click.dailyfeed.member.domain.follow.mapper;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.member.domain.follow.document.FollowingDocument;
import click.dailyfeed.member.domain.follow.projection.FollowingCountProjection;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FollowMapper {
    FollowMapper INSTANCE = Mappers.getMapper(FollowMapper.class);

    default FollowingDocument newFollowDocument(Long fromId, Long toId){
        return FollowingDocument.newFollowingBuilder()
                .fromId(fromId)
                .toId(toId)
                .build();
    }

    default FollowDto.FollowCountStatistics toFollowCountStatistics(FollowingCountProjection projection){
        return FollowDto.FollowCountStatistics.builder()
                .followingCount(projection.getFollowingCount())
                .toMemberId(projection.getToMemberId())
                .build();
    }
}
