package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceReview;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {
	List<ServiceReview> findByServiceOrder_ServiceOrderIdAndDeletedAtIsNull(Long serviceOrderId);
}
