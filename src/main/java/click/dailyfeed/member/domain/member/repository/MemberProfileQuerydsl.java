package click.dailyfeed.member.domain.member.repository;

import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.entity.QMemberProfile;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static click.dailyfeed.member.domain.member.entity.QMemberProfile.memberProfile;


// 쓸지 말지 고민... 아이고...
@RequiredArgsConstructor
@Component
public class MemberProfileQuerydsl {
    private final JPAQueryFactory queryFactory;

//    public Optional<MemberProfile> findMemberProfileWithImagesByMemberId(Long memberId) {
//        queryFactory.select(
//                    memberProfile.id, memberProfile.member.id, memberProfile.memberName, memberProfile.handle,
//                    memberProfile.displayName, memberProfile.bio, memberProfile.location, memberProfile.
//                )
//                .from(memberProfile)
//                .innerJoin(memberProfile.member).fetchJoin()
//                .leftJoin(memberProfile.profileImages).fetchJoin()
//                .where(memberProfile.member.id.eq(memberId))
//                .fetch();
//    }
}
