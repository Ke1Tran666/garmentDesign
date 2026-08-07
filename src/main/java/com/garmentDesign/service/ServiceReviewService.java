package com.garmentDesign.service;

import com.garmentDesign.dto.servicereview.ReviewableOrderResponse;
import com.garmentDesign.dto.servicereview.ServiceReviewRequest;
import com.garmentDesign.dto.servicereview.ServiceReviewResponse;

import java.util.List;

public interface ServiceReviewService {

	List<ReviewableOrderResponse> findReviewableOrders(String idUser);

	List<ServiceReviewResponse> findByUser(String idUser);

	List<ServiceReviewResponse> findPublicReviews();

	ServiceReviewResponse createByUser(Long orderId, String idUser, ServiceReviewRequest request);

	ServiceReviewResponse updateByUser(Long reviewId, String idUser, ServiceReviewRequest request);

	void deleteByUser(Long reviewId, String idUser);
}