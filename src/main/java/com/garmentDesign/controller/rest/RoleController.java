package com.garmentDesign.controller.rest;

import com.garmentDesign.dto.role.RoleUpsertRequest;
import com.garmentDesign.entity.Permission;
import com.garmentDesign.entity.Role;
import com.garmentDesign.service.RoleService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
	private final RoleService service;

	public RoleController(RoleService service) {
		this.service = service;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<Role> getAll() {
		return service.findAll();
	}

	@GetMapping("/permissions")
	@PreAuthorize("hasRole('ADMIN')")
	public List<Permission> getAllPermissions() {
		return service.findAllPermissions();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public Role getById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Role create(@RequestBody RoleUpsertRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public Role update(@PathVariable Long id, @RequestBody RoleUpsertRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id,
			@RequestParam(required = false) Long replacementRoleId) {
		service.delete(id, replacementRoleId);
		return ResponseEntity.noContent().build();
	}
}
