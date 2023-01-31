package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import java.util.List;
import java.util.function.Predicate;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import com.example.demo.dto.MemberDto;
import com.example.demo.dto.QMemberDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.Member;
import com.example.demo.entity.QMember;
import com.example.demo.entity.QTeam;
import com.example.demo.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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
                                        .orderBy(member.username.desc()).offset(1).limit(2).fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory.select(member.count(), member.age.sum(), member.age.avg(),
                                        member.age.max(), member.age.min()).from(member).fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory.select(QTeam.team.name, member.age.avg()).from(member)
                                        .join(member.team, team).groupBy(team.name).fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    @Test
    public void join() {
        List<Member> result = queryFactory.select(member).from(member).join(member.team, team)
                                        .where(team.name.eq("teamA")).fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");

    }

    /**
     * 관련 없는 것들과 조인. 세타 조인 외부조인 과거에는 불가능했고, 하이버네트 최신 버전에서 가능하게 변경됨.
     */
    @Test
    public void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory.select(member).from(member, team)
                                        .where(member.username.eq(team.name)).fetch();

        assertThat(result).extracting("username").containsExactly("teamA", "teamB");
    }

    @Test
    public void leftjoinOnFiltering() {

        List<Tuple> result = queryFactory.select(member, team).from(member)
                                        .leftJoin(member.team, team).on(team.name.eq("teamA"))
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

        List<Tuple> result = queryFactory.select(member, team).from(member).leftJoin(team)
                                        .on(member.username.eq(team.name)).fetch();

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

        Member findMember = queryFactory.selectFrom(member).join(member.team, team).fetchJoin()
                                        .where(member.username.eq("member1")).fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        // assertThat(loaded).as("페치 조인 적용 안됨").isFalse();
        assertThat(loaded).isTrue();
    }

    @Test
    @DisplayName("서브쿼리 EQ")
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member).where(member.age.eq(
                                        JPAExpressions.select(memberSub.age.max()).from(memberSub)))
                                        .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    @Test
    @DisplayName("서브쿼리 GOE")
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member).where(member.age.goe(
                                        JPAExpressions.select(memberSub.age.avg()).from(memberSub)))
                                        .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test
    @DisplayName("서브쿼리 IN")
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member).where(member.age.in(
                                        JPAExpressions.select(memberSub.age).from(memberSub).where(
                                                                        memberSub.age.gt(10))))
                                        .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    @DisplayName("select 서브 쿼리")
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member.username,
                                        JPAExpressions.select(memberSub.age.avg()).from(memberSub))
                                        .from(member).fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    /*
     * JPA 에서는 FROM 절 서브쿼리가 불가능. 복잡도가 아주 높은 쿼리 보다는 쿼리를 쪼개서 해결하는것이 좋다. 백오피스 같이 조금 느려도 되는 경우는 쪼개서 로직을
     * 태우고 프론트 같은 경우는 캐시를 사용해서 성능 높인다.
     */

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                                        .select(member.age.when(10).then("열살").when(20).then("스무살")
                                                                        .otherwise("기타"))
                                        .from(member).fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void caseBuilder() {
        List<String> result = queryFactory.select(new CaseBuilder().when(member.age.between(0, 20))
                                        .then("0~20").when(member.age.between(21, 30)).then("21~30")
                                        .otherwise("기타")).from(member).fetch();

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
                                        .from(member).fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    public void concat() {
        List<String> result = queryFactory
                                        .select(member.username.concat("_").concat(
                                                                        member.age.stringValue()))
                                        .from(member).where(member.username.eq("member1")).fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory.select(member.username).from(member).fetch();

        for (String s : result) {
            System.out.println(s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory.select(member.username, member.age).from(member).fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery(
                                        "select new com.example.demo.dto.MemberDto(m.username, m.age) from Member m",
                                        MemberDto.class).getResultList();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                                        .select(Projections.bean(MemberDto.class, member.username,
                                                                        member.age))
                                        .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                                        .select(Projections.fields(MemberDto.class, member.username,
                                                                        member.age))
                                        .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory.select(Projections.constructor(MemberDto.class,
                                        member.username, member.age)).from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findUserDtoByField() { // filed는 이름으로 매칭하므로 alias 사용 필요.
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory.select(Projections.fields(UserDto.class,
                                        member.username.as("name"),
                                        ExpressionUtils.as(JPAExpressions
                                                                        .select(memberSub.age.max())
                                                                        .from(memberSub), "age")))
                                        .from(member).fetch();

        for (UserDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() { // 타입으로 매칭하므로 alias 사용 X.
        List<UserDto> result = queryFactory.select(Projections.constructor(UserDto.class,
                                        member.username, member.age)).from(member).fetch();

        for (UserDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    /**
     * 위에 생성자, setter, 필드를 사용한 프로젝션은 런타임 레벨에서 오류가 발생하지만 컴파일 오류로 에러를 잡는게 가능하다. 단점은 DTO에 querydsl
     * 어노테이션(@QueryProjection)이 들어가므로 설계적으로 DTO가 querydsl에 의존적이게 된다. 프로젝트에 따라 유연하게 결정 필요.
     */
    @Test
    public void findUserDtoByQueryProjection() {

        List<MemberDto> result = queryFactory.select(new QMemberDto(member.username, member.age))
                                        .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void dynamicQueryBooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        com.querydsl.core.BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory.selectFrom(member).where(builder).fetch();
    }

    @Test
    public void dynamicQueryWhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
                                        // .where(usernameEq(usernameCond), ageEq(ageCond))
                                        .where(allEq(usernameCond, ageCond)).fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond != null) {
            return member.username.eq(usernameCond);
        } else {
            return null;
        }
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate() {
        // 벌크 연산은 영속성 컨텍스트의 관리하고 있는 객체와 DB의 값이 달르다.
        // 기본전략 자체가 DB값을 버린다.
        long count = queryFactory.update(member).set(member.username, "비회원")
                                        .where(member.age.lt(28)).execute();

        // 초기화를 통해서 값이 안맞는 부분에 대해 동기화한다.
        em.flush();
        em.clear();
    }

    @Test
    public void bulkAdd() {
        long count = queryFactory.update(member).set(member.age, member.age.multiply(2)).execute();
    }

    @Test
    public void bulkDelete() {
        queryFactory.delete(member).where(member.age.gt(18)).execute();
    }
    
    @Test
    public void sqlFunction() {
        queryFactory.select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
        .from(member)
        .fetch();
    }
    
    @Test
    public void sqlFunction2() {
        // ansi 표준 function 들은 대부분 querydsl에 내장되어 있음.
        queryFactory.select(member.username)
        .from(member)
        .where(member.username.eq(member.username.lower()))
        .fetch();
    }
}
