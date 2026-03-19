package com.accessrisk.repository;

import com.accessrisk.entity.RiskViolation;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskViolationRepository extends JpaRepository<RiskViolation, Long> {

    List<RiskViolation> findByUserId(Long userId);

    List<RiskViolation> findByStatus(ViolationStatus status);

    List<RiskViolation> findByUserIdAndStatus(Long userId, ViolationStatus status);

    /**
     * Used during analysis to check if this specific user+rule combo is already OPEN,
     * preventing duplicate violations from repeated analysis runs.
     */
    Optional<RiskViolation> findByUserIdAndRuleIdAndStatus(Long userId, Long ruleId, ViolationStatus status);

    long countByStatus(ViolationStatus status);

    long countBySeverity(Severity severity);
}
