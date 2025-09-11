package click.dailyfeed.member.domain.follow.mapper;

import click.dailyfeed.code.domain.member.follow.dto.FollowDto;
import click.dailyfeed.code.global.web.response.DailyfeedPage;
import click.dailyfeed.code.global.web.response.DailyfeedScrollPage;
import click.dailyfeed.member.domain.member.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    default FollowDto.FollowPage ofFollow(List<FollowDto.Follower> followers, List<FollowDto.Following> followings, Pageable pageable) {
        return FollowDto.FollowPage.builder()
                .followers(fromList(followers, pageable))
                .followings(fromList(followings, pageable))
                .build();
    }


    default <T> DailyfeedScrollPage<T> fromList(List<T> result, Pageable pageable) {
        return DailyfeedScrollPage.<T>builder()
                .content(result)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

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
