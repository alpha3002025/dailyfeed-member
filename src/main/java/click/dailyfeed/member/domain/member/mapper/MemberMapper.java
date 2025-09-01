package click.dailyfeed.member.domain.member.mapper;


import click.dailyfeed.code.domain.member.member.dto.MemberDto;
import click.dailyfeed.member.domain.member.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);

//    @Mapping(target = "followerCount", expression = "java(Long.valueOf(member.getFollowings().size()))")
//    @Mapping(target = "followingCount", expression = "java(Long.valueOf(member.getFollowers().size()))")
//    @Mapping(target = "isFollow", source = "member.followings", qualifiedByName = "isFollow")
//    MemberDto.Response toDto(Member member);
//
//    @Named("isFollow")
//    default boolean isFollow(List<Follow> follows) {
//        return true;
//    }

    @Mapping(target = "id", source = "member.id")
    @Mapping(target = "name", source = "member.name")
    @Mapping(target = "email", source = "member.email")
    MemberDto.Member ofMember(Member member);
}
