package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import com.example.demo.dto.MemberSearchCondition;
import com.example.demo.dto.MemberTeamDto;
import com.example.demo.entity.Member;
import com.example.demo.entity.Team;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MemberRepositoryTest {
    
    @Autowired
    EntityManager em;
    
    @Autowired
    MemberRepository memberRepository;
    
    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);
        
        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);
        
        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        
        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }
    
    @Test
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member m1 = new Member("member1", 10, teamA);
        Member m2 = new Member("member2", 20, teamA);

        Member m3 = new Member("member3", 30, teamB);
        Member m4 = new Member("member4", 40, teamB);

        em.persist(m1);
        em.persist(m2);
        em.persist(m3);
        em.persist(m4);
        
        MemberSearchCondition condition = MemberSearchCondition.builder()
                                        .ageGoe(35)
                                        .ageLoe(40)
                                        .teamName("teamB")
                                        .build();
        
        List<MemberTeamDto> result = memberRepository.search(condition);
        
        assertThat(result).extracting("username").containsExactly("member4");
    }
    
    @Test
    public void searchPageSimple() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member m1 = new Member("member1", 10, teamA);
        Member m2 = new Member("member2", 20, teamA);

        Member m3 = new Member("member3", 30, teamB);
        Member m4 = new Member("member4", 40, teamB);

        em.persist(m1);
        em.persist(m2);
        em.persist(m3);
        em.persist(m4);
        
        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
        
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);
        
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
        
    }
    
}
