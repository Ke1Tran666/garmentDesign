package com.garmentDesign.controller.rest;

import com.garmentDesign.dto.serviceorder.UserRemoveServiceOrderResponse;
import com.garmentDesign.dto.serviceorder.UserUpdateOrderAddressRequest;
import com.garmentDesign.dto.serviceorder.UserUpdateServiceOrderRequest;
import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import com.garmentDesign.dto.serviceorder.UserCreateServiceOrderRequest;
import java.util.List;

@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {
    private final ServiceOrderService service;

    public ServiceOrderController(ServiceOrderService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public List<ServiceOrder> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ServiceOrder getById(@PathVariable Long id) {
        return service.findById(id);
    }
    
    @GetMapping("/me")
    public List<ServiceOrder> getMine(Principal principal) {
        return service.findByUserId(
            principal.getName()
        );
    }

    @PostMapping("/me")
    public ServiceOrder createMine(
            Principal principal,
            @RequestBody
            UserCreateServiceOrderRequest request
    ) {
        return service.createByUser(
            principal.getName(),
            request
        );
    }

    @PatchMapping("/me/{orderId}")
    public ServiceOrder updateMine(
            @PathVariable Long orderId,
            Principal principal,
            @RequestBody
            UserUpdateServiceOrderRequest request
    ) {
        return service.updateByUser(
            orderId,
            principal.getName(),
            request
        );
    }
    
    @PatchMapping("/me/{orderId}/address")
    public ServiceOrder updateMyAddress(
            @PathVariable Long orderId,
            Principal principal,
            @RequestBody
            UserUpdateOrderAddressRequest request
    ) {
        return service.updateAddressByUser(
            orderId,
            principal.getName(),
            request
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/me/{orderId}")
    public ResponseEntity<UserRemoveServiceOrderResponse>
            removeMine(
                @PathVariable Long orderId,
                Principal principal
            ) {
        return ResponseEntity.ok(
            service.removeByUser(
                orderId,
                principal.getName()
            )
        );
    }
}
