package study.querydsl.dao;

import java.util.List;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

public interface MemberRepositoryCustom {

  List<MemberTeamDto> search(MemberSearchCondition condition);

}
