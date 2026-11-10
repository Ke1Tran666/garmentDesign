package com.garmentDesign.repository;

import com.garmentDesign.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
	List<Permission> findAllByOrderByIdPermissionAsc();

	Optional<Permission> findByNameIgnoreCase(String name);

	Optional<Permission> findByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCase(String code);
}
