package com.garmentDesign.controller.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentDesign.dto.service.ServiceUpsertRequest;
import com.garmentDesign.entity.Service;
import com.garmentDesign.service.ServiceService;

@RestController
@RequestMapping("/api/admin/services")
public class AdminServiceController {

	private final ServiceService service;

	public AdminServiceController(ServiceService service) {
		this.service = service;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public List<Service> getAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public Service getById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Service create(
			@RequestBody ServiceUpsertRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public Service update(
			@PathVariable Long id,
			@RequestBody ServiceUpsertRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public Service delete(@PathVariable Long id) {
		return service.delete(id);
	}
}