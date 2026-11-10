package com.garmentDesign.service;

import com.garmentDesign.dto.role.RoleUpsertRequest;
import com.garmentDesign.entity.Permission;
import com.garmentDesign.entity.Role;
import java.util.List;

public interface RoleService {
	List<Role> findAll();

	List<Permission> findAllPermissions();

	Role findById(Long id);

	Role create(RoleUpsertRequest request);

	Role update(Long id, RoleUpsertRequest request);

	void delete(Long id, Long replacementRoleId);
}
