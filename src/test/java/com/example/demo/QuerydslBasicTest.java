package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.demo.entity.Member;
import com.example.demo.entity.QMember;
import com.example.demo.entity.QTeam;
import com.example.demo.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import static com.example.demo.entity.QMember.*;
import static com.example.demo.entity.QTeam.*;

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
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
    
    @Test
    public void paging() {

        List<Member> result = queryFactory.select(member).from(member)
                                        .orderBy(member.username.desc())
                                        .offset(1)
                                        .limit(2)
                                        .fetch();
        
        assertThat(result.size()).isEqualTo(2);
    }
    
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory.select(
                                            member.count(),
                                            member.age.sum(),
                                            member.age.avg(),
                                            member.age.max(),
                                            member.age.min()
                                        )
                                        .from(member)
                                        .fetch();  
        
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    
    
    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory.select(QTeam.team.name, member.age.avg())
                                        .from(member)
                                        .join(member.team, team)
                                        .groupBy(team.name)
                                        .fetch();  
        
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
        
    }
    
    @Test
    public void join() {
        List<Member> result = queryFactory.select(member)
        .from(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();
        
        assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
        
    }
    
    /**
     * 관련 없는 것들과 조인. 세타 조인
     * 외부조인 과거에는 불가능했고, 하이버네트 최신 버전에서 가능하게 변경됨.
     */
    @Test
    public void thetaJoin() { 
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        
        List<Member> result = queryFactory.select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();
        
        assertThat(result)
           .extracting("username")
           .containsExactly("teamA", "teamB");
    }

    @Test
    public void leftjoinOnFiltering() { 
        
        List<Tuple> result = queryFactory.select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.name.eq("teamA"))
        .fetch();
        
        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }
    
    @Test
    public void joinOnNoRelation() { 
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        
        List<Tuple> result = queryFactory.select(member, team)
        .from(member)
        .leftJoin(team)
        .on(member.username.eq(team.name))
        .fetch();
        
        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }
    
    @PersistenceUnit
    EntityManagerFactory emf;
    
    @Test
    public void fetchJoin() {
        em.flush();
        em.clear();
        
        Member findMember = queryFactory
                                        .selectFrom(member)
                                        .join(member.team, team).fetchJoin()
                                        .where(member.username.eq("member1"))
                                        .fetchOne();
        
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        //assertThat(loaded).as("페치 조인 적용 안됨").isFalse();
        assertThat(loaded).isTrue();
    }
    
    @Test
    @DisplayName("서브쿼리 EQ")
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");
        
        List<Member> result = queryFactory.selectFrom(member)
        .where(member.age.eq(JPAExpressions.select(memberSub.age.max())
                                        .from(memberSub)
                                        ))
        .fetch();
        
        assertThat(result).extracting("age").containsExactly(40);
    }
    
    @Test
    @DisplayName("서브쿼리 GOE")
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");
        
        List<Member> result = queryFactory.selectFrom(member)
        .where(member.age.goe(JPAExpressions.select(memberSub.age.avg())
                                        .from(memberSub)
                                        ))
        .fetch();
        
        assertThat(result).extracting("age").containsExactly(30, 40);
    }
    
    @Test
    @DisplayName("서브쿼리 IN")
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");
        
        List<Member> result = queryFactory.selectFrom(member)
        .where(member.age.in(JPAExpressions.select(memberSub.age)
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                                        ))
        .fetch();
        
        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }
    
    @Test
    @DisplayName("select 서브 쿼리")
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        
        List<Tuple> result = queryFactory.select(member.username,
                                        JPAExpressions.select(memberSub.age.avg())
                                        .from(memberSub))
        .from(member)
        .fetch();
        
        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }
    
    /* JPA 에서는 FROM 절 서브쿼리가 불가능.
     복잡도가 아주 높은 쿼리 보다는 쿼리를 쪼개서 해결하는것이 좋다. 백오피스 같이 조금 느려도 되는 경우는 쪼개서 로직을 태우고
     프론트 같은 경우는 캐시를 사용해서 성능 높인다.
    */
    
    @Test
    public void basicCase() {
        List<String> result = queryFactory.select(member.age
                                        .when(10).then("열살")
                                        .when(20).then("스무살")
                                        .otherwise("기타"))
        .from(member)
        .fetch();
        
        for (String s : result) {
            System.out.println(s);
        }
    }
    
    @Test
    public void caseBuilder() {
        List<String> result = queryFactory.select(new CaseBuilder()
                                        .when(member.age.between(0, 20)).then("0~20")
                                        .when(member.age.between(21, 30)).then("21~30")
                                        .otherwise("기타"))
        .from(member)
        .fetch();
        
        for (String s : result) {
            System.out.println(s);
        }
    }
    
    /*
     * DB에서 이런 Case 문 처리는 권장하지 않음.
     * 
     */
    
    @Test
    public void constant() {
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();
        
        for(Tuple tuple : result) {
            System.out.println(tuple);
        }
    }
    
    @Test
    public void concat() {
        List<String> result = queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();
        
        for(String s : result) {
            System.out.println(s);
        }
    }

}
