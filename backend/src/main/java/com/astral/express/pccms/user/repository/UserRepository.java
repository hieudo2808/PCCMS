package com.astral.express.pccms.user.repository;

import com.astral.express.pccms.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);

    @Query("SELECT u FROM Users u " +
           "LEFT JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<Users> findByEmailWithRoleAndPermissions(@Param("email") String email);

    boolean existsByEmail(String email);

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
    java.util.List<Users> findActiveByRoleCode(@Param("roleCode") String roleCode);
}
