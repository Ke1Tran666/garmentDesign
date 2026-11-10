package com.garmentDesign.repository;

import com.garmentDesign.entity.Role;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByNameRole(String nameRole);

	List<Role> findAllByOrderByIdRoleAsc();

	boolean existsByNameRoleIgnoreCase(String nameRole);

	boolean existsByNameRoleIgnoreCaseAndIdRoleNot(String nameRole, Long idRole);

	@Query("SELECT COALESCE(MAX(r.idRole), 0) FROM Role r")
	Long findMaxIdRole();
}
