package com.accessrisk.repository;

import com.accessrisk.entity.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, Long> {

    /**
     * Returns only enabled rules — used by the analysis engine to skip deactivated rules.
     */
    List<RiskRule> findByActiveTrue();

    long countByActiveTrue();
}
