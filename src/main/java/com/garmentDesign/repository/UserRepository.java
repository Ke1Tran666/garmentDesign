package com.garmentDesign.repository;

import com.garmentDesign.entity.Role;
import com.garmentDesign.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
	Optional<User> findByUserCode(String userCode);

	boolean existsByUserCode(String userCode);

	Optional<User> findByIdUserAndDeletedAtIsNull(String idUser);

	boolean existsByUserCodeEndingWithIgnoreCase(String storageCode);

	long countByRole_NameRoleIgnoreCaseAndDeletedAtIsNull(String roleName);

	long countByRole_IdRole(Long roleId);

	long countByRole_IdRoleAndDeletedAtIsNull(Long roleId);

	@Query("""
			SELECT COUNT(u)
			FROM User u
			WHERE u.deletedAt IS NULL
			  AND u.role IS NOT NULL
			  AND (u.role.idRole = 1L OR UPPER(TRIM(u.role.nameRole)) = 'ADMIN')
			""")
	long countActiveAdmins();

	@Query("""
			SELECT u.role.idRole,
			       COUNT(u),
			       SUM(CASE WHEN u.deletedAt IS NULL THEN 1L ELSE 0L END)
			FROM User u
			WHERE u.role IS NOT NULL
			GROUP BY u.role.idRole
			""")
	List<Object[]> countUsersGroupedByRoleId();

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE User u
			SET u.role = :targetRole,
			    u.updatedAt = :updatedAt
			WHERE u.role.idRole = :sourceRoleId
			""")
	int reassignUsersRole(@Param("sourceRoleId") Long sourceRoleId, @Param("targetRole") Role targetRole,
			@Param("updatedAt") LocalDateTime updatedAt);
}
