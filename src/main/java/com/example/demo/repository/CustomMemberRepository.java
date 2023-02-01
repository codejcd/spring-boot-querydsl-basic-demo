package com.example.demo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.demo.dto.MemberSearchCondition;
import com.example.demo.dto.MemberTeamDto;

/**
 * Custom Spring Data JPA Repository
 * 반드시 커스텀으로 설계할 필요는 없다.
 * 공통 로직은 커스텀으로 넣고, 개별적으로 변경이 빈번한 로직들은 따로 repository로 분리하는것이 유지보수 관점에서 효율적.
 *
 */
public interface CustomMemberRepository {
    List<MemberTeamDto> search(MemberSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
