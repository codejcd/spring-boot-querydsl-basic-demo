package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import com.example.demo.dto.MemberSearchCondition;
import com.example.demo.dto.MemberTeamDto;
import com.example.demo.dto.QMemberTeamDto;
import com.example.demo.entity.Member;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import static com.example.demo.entity.QMember.*;
import static com.example.demo.entity.QTeam.*;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;
    
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }
    
    public void save(Member member) {
        em.persist(member);
    }
    
    public Optional<Member> findById(Long id) {
        Member finMember = em.find(Member.class, id);
        return Optional.ofNullable(finMember);
    }
    
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }
    
    public List<Member> findAllQueryDsl() {
        return queryFactory.selectFrom(member).fetch();
    }
    
    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                                        .setParameter("username", username).getResultList();
    }
    
    public List<Member> findByUsernameQueryDsl(String username) {
        return queryFactory.selectFrom(member).where(member.username.eq(username)).fetch();
    }
    
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        
        if(StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        
        if(StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        
        if(condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        
        if(condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        
        return queryFactory.select(new QMemberTeamDto(
                                        member.id.as("memberId"),
                                        member.username,
                                        member.age,
                                        team.id.as("teamId"),
                                        team.name.as("teamName")))
                                        .from(member)
                                        .leftJoin(member.team, team)
                                        .where(builder)
                                        .fetch();
    }
    
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory.select(new QMemberTeamDto(
                                        member.id.as("memberId"),
                                        member.username,
                                        member.age,
                                        team.id.as("teamId"),
                                        team.name.as("teamName")))
                                        .from(member)
                                        .leftJoin(member.team, team)
                                        .where(
                                               usernameEq(condition.getUsername()),
                                               teamNameEq(condition.getTeamName()),
                                               ageGoe(condition.getAgeGoe()),
                                               ageLoe(condition.getAgeLoe())
                                               )
                                        .fetch();
    }
    
    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }
    
    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }
    
    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }
    
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
    
    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        return ageGoe(ageLoe).and(ageGoe(ageGoe));
    }
}
