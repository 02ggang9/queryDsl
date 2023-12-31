package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;

  JPAQueryFactory queryFactory;


  @BeforeEach
  void setUp() {
    queryFactory = new JPAQueryFactory(em);
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  public void startJPQL() {
    // member1을 찾아라
    Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void startQuerydsl() {

    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1")) // 파라미터 바인딩 처리
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");

  }

  @Test
  public void search() {
    Member findMember = queryFactory.selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void searchAndParam() {
    Member findMember = queryFactory.selectFrom(member)
        .where(
            member.username.eq("member1"),
            member.age.eq(10)
        )
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void resultFetch() {
//    List<Member> fetch = queryFactory
//        .selectFrom(member)
//        .fetch();
//
//    Member fetchOne = queryFactory
//        .selectFrom(member)
//        .fetchOne();

//    List<Member> fetch = queryFactory
//        .selectFrom(member)
//        .fetch();

//    queryFactory
//        .selectFrom(member)
//        .fetchCount();
  }

  // 1. 회원 나이 내림차순
  // 2. 회원 이름 올림차순
  // 단, 2에서 회원 이름이 없으면 마지막에 출력(null last)
  @Test
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(
            member.age.desc(),
            member.username.asc().nullsLast()
        )
        .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  @DisplayName("페이징1")
  public void paging1() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("집합")
  public void aggregation() {
    List<Tuple> result = queryFactory
        .select(
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

  // 팀의 이름과 각 팀의 평균 연령을 구해라.
  @Test
  public void group() throws Exception {
    List<Tuple> result = queryFactory
        .select(
            team.name,
            member.age.avg()
        )
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
  public void join() throws Exception {
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  @Test
  public void theta_join() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  @Test
  public void join_on_filtering() throws Exception {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team).on(team.name.eq("teamA"))
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void join_on_no_relation() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team).on(member.username.eq(team.name))
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  public void fetchJoinNo() throws Exception {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).isFalse();

  }

  @Test
  public void fetchJoinUse() throws Exception {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).isTrue();
  }

  // 나이가 가장 많은 회원 조회
  @Test
  public void subQuery() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(40);
  }

  // 나이가 평균 이상인 회원
  @Test
  public void subQueryGoe() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.goe(
            select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(30, 40);
  }


  @Test
  public void subQueryIn() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.in(
            select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(20, 30, 40);
  }

  @Test
  public void selectSubQuery() throws Exception {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = queryFactory
        .select(
            member.username,
            select(memberSub.age.avg())
                .from(memberSub)
        )
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void basicCase() throws Exception {
    List<String> result = queryFactory
        .select(
            member.age
                .when(10).then("열살")
                .when(20).then("스무살")
                .otherwise("기타")
        )
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  // 이걸 정말 써야하나?
  // 권장 -> DB에서 하지 말고 DB는 rowData를 필터링, 그룹화, 계산 등은 가능
  // 10살이야 이런게 전환하는 것은 DB에서 NONO..
  // 프리젠테이션 계층에서 해결을 할 것!!
  @Test
  public void complexCase() throws Exception {
    List<String> result = queryFactory
        .select(
            new CaseBuilder()
                .when(member.age.between(0, 20)).then("0~20살")
                .when(member.age.between(21, 30)).then("21~30살")
                .otherwise("기타")
        )
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void constant() throws Exception {
    List<Tuple> result = queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void concat() throws Exception {
//   queryFactory.select(member.username.concat("_").concat(member.age)) 타입이 달라서 안됨
    List<String> result = queryFactory
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void simpleProjection() throws Exception {
    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void tupleProjection() throws Exception {
    List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      System.out.println("username = " + username);
      System.out.println("age = " + age);
    }
  }

  @Test
  public void findDtoByJPQL() throws Exception {
    List<MemberDto> result = em.createQuery(
            "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
        .getResultList();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoBySetter() throws Exception {
    List<MemberDto> result = queryFactory
        .select(
            Projections.bean(
                MemberDto.class,
                member.username,
                member.age
            )
        )
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoByField() throws Exception {
    List<MemberDto> result = queryFactory
        .select(
            Projections.fields(
                MemberDto.class,
                member.username,
                member.age
            )
        )
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findDtoByConstructor() throws Exception {
    List<MemberDto> result = queryFactory
        .select(
            Projections.constructor(
                MemberDto.class,
                member.username,
                member.age
            )
        )
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void findUserDto() throws Exception {
    List<UserDto> result = queryFactory
        .select(
            Projections.fields(
                UserDto.class,
                member.username.as("name"),
                member.age
            )
        )
        .from(member)
        .fetch();

    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }

  @Test
  public void findUserDtoBySubQuery() throws Exception {
    QMember memberSub = new QMember("memberSub");
    List<UserDto> fetch = queryFactory
        .select(Projections.fields(UserDto.class,
                member.username.as("name"),
                ExpressionUtils.as(
                    JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")
            )
        ).from(member)
        .fetch();

    for (UserDto userDto : fetch) {
      System.out.println("userDto = " + userDto);
    }
  }

  // 장점도 있지만 단점도 있음
  // constructor -> 컴파일 오류를 못잡고 런타임 오류가 발생한다
  // 위 방식에서 매개변수로 추가로 들어와도 컴파일 오류를 못잡지만 아래 방식은 가능
  // 실무에서 고민거니는 컴파일 시점에서도 잡아주고 좋은데.
  // Q 파일 만들어줘야 하고 쿼리DSL에 관한 의존성을 가지게 됨.
  // DTO는 여러 레이어에서 사용됨(Controller, service) 등등..
  // DTO가 DSL에 의존하게 되어 순수하지 않음.. 아키텍처에서 문제가 됨
  @Test
  public void findDtoByQueryProjection() throws Exception {
    List<MemberDto> result = queryFactory
        .select(new QMemberDto(member.username, member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  public void dynamicQuery_BooleanBuilder() throws Exception {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  //null 방어를 하기 위해서는 new BooleanBuilder() 매개변수 안에 초기 값을 넣어줄 수 있음
  //new BooleanBuilder(member.username.eq(usernameParam))

  private List<Member> searchMember1(String usernameParam, Integer ageParam) {

    BooleanBuilder booleanBuilder = new BooleanBuilder();
    if (usernameParam != null) {
      booleanBuilder.and(member.username.eq(usernameParam));
    }

    if (ageParam != null) {
      booleanBuilder.and(member.age.eq(ageParam));
    }

    return queryFactory
        .selectFrom(member)
        .where(booleanBuilder)
        .fetch();
  }

  // where 문에 null이 들어가면 무시됨.
  // 조립도 가능
  // 메서드를 다른 쿼리에서도 재활용 할 수 있다.
  // 쿼리 자체의 가독성이 높아진다.
  @Test
  public void dynamicQuery_WhereParam() throws Exception {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember2(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember2(String usernameParam, Integer ageParam) {
    return queryFactory
        .selectFrom(member)
        .where(usernameEq(usernameParam), ageEq(ageParam))
        .fetch();
  }

  private BooleanExpression usernameEq(String usernameParam) {
    return usernameParam != null ? member.username.eq(usernameParam) : null;
  }

  private BooleanExpression ageEq(Integer ageParam) {
    return ageParam != null ? member.age.eq(ageParam) : null;
  }

  private BooleanExpression allEq(String usernameParam, Integer ageParam) {
    return usernameEq(usernameParam).and(ageEq(ageParam));
  }

  // 아래의 벌크 쿼리가 날아가면 영속성 컨텍스트 무시하고 바로 update를 치기 때문에
  // 영속성 컨텍스트와의 상태가 달라짐.
  // 수정하고 select 해서 가져와도 영속성 컨텍스트에 값이 있기 때문에 가져온 값을 버림
  // 원하던 결과랑 다를 수 있다!

  // 그래서 벌크 연산 연산을 하면 em.flush()랑 em.clear()로 컨텍스트 비워야 한다.
  @Test
  public void bulkUpdate() throws Exception {
    long count = queryFactory
        .update(member)
        .set(member.username, "비회원")
        .where(member.age.lt(28))
        .execute();

    em.flush();
    em.clear();

    List<Member> result = queryFactory
        .selectFrom(member)
        .fetch();

    for (Member member1 : result) {
      System.out.println("member1 = " + member1);
    }
  }

  @Test
  public void bulkAdd() throws Exception {
    long execute = queryFactory
        .update(member)
        .set(member.age, member.age.add(1))
        .execute();
  }

  @Test
  public void bulkDelete() throws Exception {
    long count = queryFactory
        .delete(member)
        .where(member.age.gt(18))
        .execute();
  }

  @Test
  public void sqlFunction() throws Exception {
    List<String> result = queryFactory
        .select
            (
                Expressions.stringTemplate(
                    "function('replace', {0}, {1}, {2})",
                    member.username,
                    "member", "M"
                )
            )
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

}
