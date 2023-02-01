package com.example.demo.repository;

import static com.example.demo.entity.QMember.member;
import static com.example.demo.entity.QTeam.team;
import java.util.List;
import javax.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import com.example.demo.dto.MemberSearchCondition;
import com.example.demo.dto.MemberTeamDto;
import com.example.demo.dto.QMemberTeamDto;
import com.example.demo.entity.Member;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

public class MemberRepositoryImpl implements CustomMemberRepository {

    private final JPAQueryFactory queryFactory;
    
    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }
    
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
                                    Pageable pageable) {
        
        List<MemberTeamDto> content = queryFactory.select(new QMemberTeamDto(
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
                                        .offset(pageable.getOffset())
                                        .limit(pageable.getPageSize())
                                        .fetch();
        
            /*long total = queryFactory.select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                   usernameEq(condition.getUsername()),
                   teamNameEq(condition.getTeamName()),
                   ageGoe(condition.getAgeGoe()),
                   ageLoe(condition.getAgeLoe())
                   ).fetchCount();*/
        
         JPAQuery<Member> countQuery = queryFactory.select(member)
        .from(member)
        .leftJoin(member.team, team)
        .where(
               usernameEq(condition.getUsername()),
               teamNameEq(condition.getTeamName()),
               ageGoe(condition.getAgeGoe()),
               ageLoe(condition.getAgeLoe())
               );
        
            return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
            //return new PageImpl<>(content, pageable, total);
    }
    
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition,
                                    Pageable pageable) {
        
             QueryResults<MemberTeamDto> results = queryFactory.select(new QMemberTeamDto(
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
                                        .offset(pageable.getOffset())
                                        .limit(pageable.getPageSize())
                                        .fetchResults();
            
            List<MemberTeamDto> content = results.getResults();
            long total = results.getTotal();
            
            return new PageImpl<>(content, pageable, total);
    }
    
    @Override
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
    
}
