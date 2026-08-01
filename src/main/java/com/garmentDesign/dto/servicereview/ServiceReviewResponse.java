package com.garmentDesign.dto.servicereview;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceReviewResponse(
    Long reviewId,
    Long serviceOrderId,
    String orderCode,
    String productName,
    String serviceName,
    String reviewerName,
    String companyName,
    Integer rating,
    String reviewContent,
    Boolean isPublic,
    LocalDate completedDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}