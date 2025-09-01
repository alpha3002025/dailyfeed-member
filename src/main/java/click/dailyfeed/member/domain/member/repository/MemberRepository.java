package click.dailyfeed.member.domain.member.repository;

import click.dailyfeed.member.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findByEmail(String email);
    List<Member> findByIdIn(List<Long> ids);
}
