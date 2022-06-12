package com.example.demo.member.repository;

import com.example.demo.member.entity.MemberRole;
import com.example.demo.member.entity.MemberRoleType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface MemberRoleRepository {

    Optional<MemberRole> findByName(MemberRoleType name);

//    @Transactional
//    @Query(value = "select * from member_roles where role = :membNo", nativeQuery = true)
//    Optional<MemberRole> findByRole(@Param("membNo") Long membNo);
}
