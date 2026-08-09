package com.garmentDesign.dto.servicereview;

public record ServiceReviewRequest(Integer rating, String reviewContent, String companyName, Boolean isPublic) {
}