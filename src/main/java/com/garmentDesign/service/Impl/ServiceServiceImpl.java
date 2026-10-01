package com.garmentDesign.service.Impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.garmentDesign.dto.service.ServiceUpsertRequest;
import com.garmentDesign.entity.Service;
import com.garmentDesign.repository.ServiceRepository;
import com.garmentDesign.service.ServiceService;

@org.springframework.stereotype.Service
public class ServiceServiceImpl implements ServiceService {

	private static final String ACTIVE = "active";
	private static final String INACTIVE = "inactive";

	private final ServiceRepository repository;

	public ServiceServiceImpl(ServiceRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<Service> findPublicServices() {
		return repository
				.findByDeletedAtIsNullAndStatusIgnoreCaseOrderByCreatedAtDesc(
						ACTIVE);
	}

	@Override
	public Service findPublicById(Long id) {
		return repository
				.findByServiceIdAndDeletedAtIsNullAndStatusIgnoreCase(
						id,
						ACTIVE)
				.orElseThrow(() ->
						new RuntimeException(
								"Dịch vụ không tồn tại hoặc đã ngừng hoạt động."));
	}

	@Override
	public List<Service> findAll() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	@Override
	public Service findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() ->
						new RuntimeException(
								"Không tìm thấy dịch vụ với id: " + id));
	}

	@Override
	@Transactional
	public Service create(ServiceUpsertRequest request) {
		ValidatedServiceData data = validateRequest(request);

		if (repository.existsByServiceCodeIgnoreCase(data.serviceCode())) {
			throw new RuntimeException("Mã dịch vụ đã tồn tại.");
		}

		Service service = new Service();

		applyData(service, data);

		return repository.save(service);
	}

	@Override
	@Transactional
	public Service update(Long id, ServiceUpsertRequest request) {
		Service service = findById(id);

		if (service.getDeletedAt() != null) {
			throw new RuntimeException(
					"Không thể chỉnh sửa dịch vụ đã xóa.");
		}

		ValidatedServiceData data = validateRequest(request);

		if (repository.existsByServiceCodeIgnoreCaseAndServiceIdNot(
				data.serviceCode(),
				id)) {
			throw new RuntimeException("Mã dịch vụ đã tồn tại.");
		}

		applyData(service, data);

		return repository.save(service);
	}

	@Override
	@Transactional
	public Service delete(Long id) {
		Service service = findById(id);

		if (service.getDeletedAt() != null) {
			throw new RuntimeException("Dịch vụ đã được xóa trước đó.");
		}

		service.setDeletedAt(LocalDateTime.now());
		service.setStatus(INACTIVE);

		return repository.save(service);
	}

	private ValidatedServiceData validateRequest(
			ServiceUpsertRequest request) {
		if (request == null) {
			throw new RuntimeException(
					"Thông tin dịch vụ không hợp lệ.");
		}

		String serviceCode = normalizeRequired(
				request.serviceCode(),
				"Mã dịch vụ");

		String serviceName = normalizeRequired(
				request.serviceName(),
				"Tên dịch vụ");

		String unitType = normalizeRequired(
				request.unitType(),
				"Đơn vị tính");

		BigDecimal basePrice = request.basePrice();

		if (basePrice == null ||
				basePrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException(
					"Giá cơ bản phải lớn hơn hoặc bằng 0.");
		}

		String status = normalizeStatus(request.status());

		return new ValidatedServiceData(
				serviceCode,
				serviceName,
				unitType,
				basePrice,
				normalizeOptional(request.description()),
				normalizeOptional(request.tags()),
				status);
	}

	private String normalizeRequired(
			String value,
			String fieldName) {
		String normalized = value == null ? "" : value.trim();

		if (normalized.isBlank()) {
			throw new RuntimeException(
					fieldName + " không được để trống.");
		}

		return normalized;
	}

	private String normalizeOptional(String value) {
		if (value == null) {
			return null;
		}

		String normalized = value.trim();

		return normalized.isBlank() ? null : normalized;
	}

	private String normalizeStatus(String value) {
		String status = value == null
				? ACTIVE
				: value.trim().toLowerCase(Locale.ROOT);

		if (!ACTIVE.equals(status) && !INACTIVE.equals(status)) {
			throw new RuntimeException(
					"Trạng thái dịch vụ không hợp lệ.");
		}

		return status;
	}

	private void applyData(
			Service service,
			ValidatedServiceData data) {
		service.setServiceCode(data.serviceCode());
		service.setServiceName(data.serviceName());
		service.setUnitType(data.unitType());
		service.setBasePrice(data.basePrice());
		service.setDescription(data.description());
		service.setTags(data.tags());
		service.setStatus(data.status());
	}

	private record ValidatedServiceData(
			String serviceCode,
			String serviceName,
			String unitType,
			BigDecimal basePrice,
			String description,
			String tags,
			String status) {
	}
}