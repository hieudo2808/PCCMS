package com.astral.express.pccms.user.repository;

import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID>, JpaSpecificationExecutor<Users> {
    Optional<Users> findByEmail(String email);

    @Query("SELECT u FROM Users u " +
           "LEFT JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<Users> findByEmailWithRoleAndPermissions(@Param("email") String email);

    @Query("SELECT u FROM Users u " +
           "LEFT JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<Users> findByIdWithRoleAndPermissions(@Param("id") UUID id);

    boolean existsByEmail(String email);

    Optional<Users> findByPhone(String phone);

    @Query("""
            SELECT u FROM Users u
            JOIN FETCH u.role r
            WHERE REPLACE(REPLACE(REPLACE(u.phone, ' ', ''), '.', ''), '-', '') = :normalizedPhone
            """)
    Optional<Users> findByNormalizedPhone(@Param("normalizedPhone") String normalizedPhone);

    @Query(value = """
            SELECT u.* FROM users u
            INNER JOIN roles r ON r.id = u.role_id
            WHERE r.code = :roleCode
              AND u.status_code::text = 'ACTIVE'
              AND u.deleted_at IS NULL
            ORDER BY u.full_name ASC
            """, nativeQuery = true)
    List<Users> findActiveByRoleCode(@Param("roleCode") String roleCode);

    @Query("SELECT u FROM Users u JOIN FETCH u.role r WHERE u.statusCode = :statusCode AND r.code IN :roleCodes")
    List<Users> findScheduleStaffOptions(@Param("statusCode") UserStatus statusCode, @Param("roleCodes") List<String> roleCodes);
}
