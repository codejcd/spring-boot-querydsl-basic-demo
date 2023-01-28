package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.entity.Member;
import com.example.demo.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import static com.example.demo.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory; // 멀티스레드 환경에서 동시성 문제가 없게 설계되어 있음.

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

        queryFactory = new JPAQueryFactory(em);

    }

    @Test
    public void startJPQL() {
        Member m = em.createQuery("select m from Member m where m.username = :username",
                                        Member.class).setParameter("username", "member1")
                                        .getSingleResult();

        assertThat(m.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        // Q 타입은 static import 권장. 같은 테이블 조인 시에는 선언해서 써야함.
        Member findMember = queryFactory.select(member).from(member)
                                        .where(member.username.eq("member1")).fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() {
        // Q 타입은 static import 권장. 같은 테이블 조인 시에는 선언해서 써야함.
        Member findMember = queryFactory.select(member).from(member)
                                        .where(member.username.eq("member1").and(member.age.eq(10)))
                                        .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch() {
        // Q 타입은 static import 권장. 같은 테이블 조인 시에는 선언해서 써야함.
        List<Member> memberList = queryFactory.select(member).from(member).fetch();

        Member fecthFirst = queryFactory.select(member).from(member).fetchFirst();

        QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();


    }

    @Test
    public void count() {
        Long totalCount = queryFactory.select(member.count()).from(member).fetchOne();

        assertThat(totalCount).isEqualTo(4);
    }

    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.select(member).from(member).where(member.age.eq(100))
                                        .orderBy(member.age.desc(), member.username.asc()
                                                                        .nullsLast()) // 회원 명이 존재하지
                                                                                      // 않으면(null)
                                                                                      // 마지막(last)에
                                                                                      // 출력
                                        .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member5.getUsername()).isEqualTo("member6");
        assertThat(memberNull).isNull();
    }

}
