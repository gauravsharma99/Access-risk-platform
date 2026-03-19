package com.accessrisk.repository;

import com.accessrisk.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    List<RolePermission> findByRoleIdIn(List<Long> roleIds);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    /**
     * Returns the distinct set of permission IDs granted to a user through any of their roles.
     * Used by the risk analysis engine to compute effective permissions.
     */
    @Query("""
        SELECT DISTINCT rp.permissionId
        FROM RolePermission rp
        WHERE rp.roleId IN (
            SELECT ur.roleId FROM UserRole ur WHERE ur.userId = :userId
        )
        """)
    Set<Long> findEffectivePermissionIdsByUserId(@Param("userId") Long userId);
}
