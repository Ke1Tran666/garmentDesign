package com.garmentDesign.service;

import java.util.List;

import com.garmentDesign.dto.service.ServiceUpsertRequest;
import com.garmentDesign.entity.Service;

public interface ServiceService {

	List<Service> findPublicServices();

	Service findPublicById(Long id);

	List<Service> findAll();

	Service findById(Long id);

	Service create(ServiceUpsertRequest request);

	Service update(Long id, ServiceUpsertRequest request);

	Service delete(Long id);
}