package click.dailyfeed.member.domain.member.repository.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


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
