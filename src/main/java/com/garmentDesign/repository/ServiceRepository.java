package com.garmentDesign.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.garmentDesign.entity.Service;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

	List<Service> findAllByOrderByCreatedAtDesc();

	List<Service> findByDeletedAtIsNullAndStatusIgnoreCaseOrderByCreatedAtDesc(
			String status);

	Optional<Service> findByServiceIdAndDeletedAtIsNullAndStatusIgnoreCase(
			Long serviceId,
			String status);

	boolean existsByServiceCodeIgnoreCase(String serviceCode);

	boolean existsByServiceCodeIgnoreCaseAndServiceIdNot(
			String serviceCode,
			Long serviceId);
}