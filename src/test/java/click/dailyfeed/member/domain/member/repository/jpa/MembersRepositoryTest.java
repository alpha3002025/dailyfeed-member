package click.dailyfeed.member.domain.member.repository.jpa;


import click.dailyfeed.code.domain.member.member.exception.MemberNotFoundException;
import click.dailyfeed.member.domain.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles({"local-test"})
@SpringBootTest
public class MembersRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private final static Logger log = LoggerFactory.getLogger(MembersRepositoryTest.class);

    @Transactional
    @Test
    public void find_member_profile_by_member_id(){
        Member result = memberRepository.findFirstByEmailFetchJoin("case3_A@gmail.com")
                        .orElseThrow(() -> new MemberNotFoundException());

        log.info("email = {}, member id = {}", result.getMemberEmails().get(0).getEmail(), result.getId());
        log.info("email = {}, member id = {}", result.getMemberEmails().get(0).getEmail(), result.getId());
    }
}