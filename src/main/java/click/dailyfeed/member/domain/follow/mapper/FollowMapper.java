package click.dailyfeed.member.domain.follow.mapper;

import click.dailyfeed.member.domain.follow.document.FollowingDocument;
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
}
