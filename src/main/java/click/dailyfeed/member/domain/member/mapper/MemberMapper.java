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

    @Mapping(target = "id", source = "member.id")
    @Mapping(target = "name", source = "member.name")
    @Mapping(target = "email", source = "member.email")
    MemberDto.Member ofMember(Member member);
}
