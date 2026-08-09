package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceReview;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {

	List<ServiceReview> findByUser_IdUserAndDeletedAtIsNullOrderByCreatedAtDesc(String idUser);

	List<ServiceReview> findByIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDesc();

	Optional<ServiceReview> findByReviewIdAndUser_IdUserAndDeletedAtIsNull(Long reviewId, String idUser);

	Optional<ServiceReview> findByServiceOrder_ServiceOrderIdAndUser_IdUserAndDeletedAtIsNull(Long serviceOrderId,
			String idUser);

	boolean existsByServiceOrder_ServiceOrderIdAndUser_IdUserAndDeletedAtIsNull(Long serviceOrderId, String idUser);

	void deleteByServiceOrder_ServiceOrderId(Long serviceOrderId);
}