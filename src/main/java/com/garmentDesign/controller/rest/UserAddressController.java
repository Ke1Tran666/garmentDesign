package com.garmentDesign.controller.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.service.UserAddressService;

@RestController
@RequestMapping("/api/user-addresses")
public class UserAddressController {
	private final UserAddressService service;

	public UserAddressController(UserAddressService service) {
		this.service = service;
	}

	@GetMapping
	public List<UserAddress> getAll() {
		return service.findAll();
	}

	@GetMapping("/me")
	public List<UserAddress> getMine(Authentication authentication) {
		return service.findByUserId(authentication.getName());
	}

	@GetMapping("/{id}")
	public UserAddress getById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	public UserAddress create(@RequestBody UserAddress data) {
		return service.save(data);
	}

	@PostMapping("/me")
	public UserAddress createMine(Authentication authentication, @RequestBody UserAddress data) {
		return service.createByUser(authentication.getName(), data);
	}

	@PutMapping("/me/{addressId}")
	public UserAddress updateMine(Authentication authentication, @PathVariable Long addressId,
			@RequestBody UserAddress data) {
		return service.updateByUser(authentication.getName(), addressId, data);
	}

	@PutMapping("/me/default/{addressId}")
	public UserAddress setMyDefaultAddress(Authentication authentication, @PathVariable Long addressId) {
		return service.setDefaultAddress(authentication.getName(), addressId);
	}

	@DeleteMapping("/me/{addressId}")
	public ResponseEntity<Void> deleteMine(Authentication authentication, @PathVariable Long addressId) {
		service.deleteByUser(authentication.getName(), addressId);

		return ResponseEntity.noContent().build();
	}
}
