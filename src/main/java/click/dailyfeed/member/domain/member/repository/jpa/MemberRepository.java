package click.dailyfeed.member.domain.member.repository.jpa;

import click.dailyfeed.member.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByIdIn(List<Long> ids);

    @Query("select m from Member m left join fetch m.memberEmails me where me.email = :email and me.isActive = true and me.deactivatedAt is null")
    Optional<Member> findFirstByEmailFetchJoin(String email);

    @Query("select m from Member m left join fetch m.memberEmails me where m.id = :memberId and me.isActive = true")
    List<Member> findByIdFetchJoin(Long memberId);
}
