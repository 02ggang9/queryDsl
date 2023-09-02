package study.querydsl.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

  List<Member> findByUsername(String username);

}
