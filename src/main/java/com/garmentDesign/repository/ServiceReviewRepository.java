package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {
}
