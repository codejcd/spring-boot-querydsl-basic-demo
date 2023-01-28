package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.entity.Member;
import com.example.demo.entity.QMember;
import com.example.demo.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    
    @Autowired
    EntityManager em;
    
    @BeforeEach
    public void before() {
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
        
    }
    
    @Test
    public void startJPQL() {
        Member m = em.createQuery("select m from Member m where m.username = :username", Member.class)
        .setParameter("username", "member1")
        .getSingleResult();
        
        assertThat(m.getUsername()).isEqualTo("member1");
    }
    
    @Test
    public void startQuerydsl() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember m = new QMember("m");
        
        Member findMember = queryFactory
                                        .select(m)
                                        .from(m)
                                        .where(m.username.eq("member1"))
                                        .fetchOne();
        
        assertThat(findMember.getUsername()).isEqualTo("member1");
         
    }
    
}
