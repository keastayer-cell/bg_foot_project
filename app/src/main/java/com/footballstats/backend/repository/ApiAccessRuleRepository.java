package com.footballstats.backend.repository;

import com.footballstats.backend.domain.ApiAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApiAccessRuleRepository extends JpaRepository<ApiAccessRule, Long> {

    @Query("""
        select r
        from ApiAccessRule r
        join fetch r.role role
        where r.active = true
    """)
    List<ApiAccessRule> findAllActiveWithRole();
}