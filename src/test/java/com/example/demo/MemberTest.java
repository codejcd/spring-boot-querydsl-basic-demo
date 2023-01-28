package com.example.demo;

import java.util.List;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import com.example.demo.entity.Member;
import com.example.demo.entity.Team;

@SpringBootTest
@Transactional
@Commit
public class MemberTest {
    
    @Autowired
    EntityManager em;
    
    @Test
    public void testEntity() {
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
        
        em.flush();
        em.clear();
        
        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();
        
        members.stream().forEach(System.out::println);
    }
}
