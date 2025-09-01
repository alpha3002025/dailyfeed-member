package click.dailyfeed.member.domain.follow.mapper;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.member.domain.member.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

import java.util.List;

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

    @Mapping(target = "followers", source = "followers")
    @Mapping(target = "followings", source = "followings")
    FollowDto.Follow ofFollow(List<FollowDto.Follower> followers, List<FollowDto.Following> followings);


    default <T> DailyfeedPage<T> fromPage(Page<T> page) {
        return DailyfeedPage.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
