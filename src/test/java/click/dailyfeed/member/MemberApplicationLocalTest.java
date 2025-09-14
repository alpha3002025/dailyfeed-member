package click.dailyfeed.member;

import click.dailyfeed.member.domain.member.entity.Member;
import click.dailyfeed.member.domain.member.repository.MemberRepository;
import click.dailyfeed.member.fixtures.MemberDataSet001;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles({"local-test"})
@SpringBootTest
public class MemberApplicationLocalTest {
    @Autowired
    private MemberDataSet001 memberDataSet001;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void TEST__멤버리스트_조회(){
        memberDataSet001.init();

        List<Member> members = memberRepository.findAll();
        members.forEach(member -> {
            System.out.println(member.getName());
        });
    }
}
