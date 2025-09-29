package click.dailyfeed.member.domain.member.repository;

import click.dailyfeed.member.domain.member.entity.MemberEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberEmailsRepository extends JpaRepository<MemberEmail, Long> {

}
