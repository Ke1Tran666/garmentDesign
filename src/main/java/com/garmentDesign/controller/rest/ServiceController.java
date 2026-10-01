package com.garmentDesign.controller.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentDesign.entity.Service;
import com.garmentDesign.service.ServiceService;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

	private final ServiceService service;

	public ServiceController(ServiceService service) {
		this.service = service;
	}

	@GetMapping
	public List<Service> getAll() {
		return service.findPublicServices();
	}

	@GetMapping("/{id}")
	public Service getById(@PathVariable Long id) {
		return service.findPublicById(id);
	}
}