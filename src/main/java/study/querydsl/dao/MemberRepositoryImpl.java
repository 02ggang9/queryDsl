package study.querydsl.dao;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

public class MemberRepositoryImpl implements MemberRepositoryCustom{

  private final JPAQueryFactory queryFactory;

  public MemberRepositoryImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public List<MemberTeamDto> search(MemberSearchCondition condition) {
    return queryFactory
        .select(new QMemberTeamDto(
            member.id.as("memberId"),
            member.username,
            member.age,
            team.id.as("teamId"),
            team.name.as("teamName")
        ))
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGeo(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .fetch();
  }

  private BooleanExpression usernameEq(String username) {
    return !hasText(username) ? null : member.username.eq(username);
  }

  private BooleanExpression teamNameEq(String teamName) {
    return !hasText(teamName) ? null : team.name.eq(teamName);


  }

  private BooleanExpression ageGeo(Integer ageGoe) {
    return ageGoe != null ? member.age.goe(ageGoe) : null;
  }

  private BooleanExpression ageLoe(Integer ageLoe) {
    return ageLoe != null ? member.age.loe(ageLoe) : null;
  }

}
