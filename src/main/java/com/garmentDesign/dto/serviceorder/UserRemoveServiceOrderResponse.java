package com.garmentDesign.dto.serviceorder;

import com.garmentDesign.entity.ServiceOrder;

public record UserRemoveServiceOrderResponse(
    Long orderId,
    String action,
    ServiceOrder order,
    String message
) {
}