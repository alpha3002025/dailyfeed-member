package click.dailyfeed.member.domain.member.repository.jpa;


import click.dailyfeed.member.domain.member.entity.MemberProfile;
import click.dailyfeed.member.domain.member.repository.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@ActiveProfiles({"local-test"})
@SpringBootTest
public class MemberProfileRepositoryTest {

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    private final static Logger log = LoggerFactory.getLogger(MemberProfileRepositoryTest.class);

    @Transactional
    @Test
    public void find_member_profile_by_member_id(){
        MemberProfile mp = memberProfileRepository.findMemberProfileByMemberId(4L).orElseThrow(RuntimeException::new);

        log.info("mp id = {}, mp url = {}, member id = {}", mp.getId(), mp.getAvatarUrl(), mp.getMember().getId());
        log.info("mp id = {}, mp url = {}, member id = {}", mp.getId(), mp.getAvatarUrl(), mp.getMember().getId());
    }


}
