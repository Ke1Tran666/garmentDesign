package com.garmentDesign.dto.service;

import java.math.BigDecimal;

public record ServiceUpsertRequest(
		String serviceCode,
		String serviceName,
		String unitType,
		BigDecimal basePrice,
		String description,
		String tags,
		String status) {
}