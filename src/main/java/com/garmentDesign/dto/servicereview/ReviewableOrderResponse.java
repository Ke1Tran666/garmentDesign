package com.garmentDesign.dto.servicereview;

import java.time.LocalDate;

public record ReviewableOrderResponse(
    Long serviceOrderId,
    String orderCode,
    String productName,
    String productImage,
    String serviceName,
    LocalDate completedDate,
    ServiceReviewResponse review
) {}