package com.garmentDesign.service.Impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.dto.serviceorder.UserUpdateOrderAddressRequest;
import com.garmentDesign.dto.serviceorder.UserUpdateServiceOrderRequest;
import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.repository.ServiceOrderRepository;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.service.ServiceOrderService;

import jakarta.transaction.Transactional;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {
	private final ServiceOrderRepository repository;
	private final UserAddressRepository addressRepository;

	public ServiceOrderServiceImpl(
	        ServiceOrderRepository repository,
	        UserAddressRepository addressRepository
	) {
	    this.repository = repository;
	    this.addressRepository =
	            addressRepository;
	}

    @Override
    public List<ServiceOrder> findAll() {
        return repository.findAll();
    }

    @Override
    public ServiceOrder findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
    }

    @Override
    public ServiceOrder save(ServiceOrder data) {
        return repository.save(data);
    }

    @Override
    public ServiceOrder update(Long id, ServiceOrder data) {
        ServiceOrder oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }
    
    @Override
    @Transactional
    public ServiceOrder updateByUser(
            Long orderId,
            String idUser,
            UserUpdateServiceOrderRequest request
    ) {
        ServiceOrder currentOrder = repository
                .findById(orderId)
                .orElseThrow(
                    () -> new RuntimeException(
                        "Không tìm thấy đơn hàng."
                    )
                );

        if (
            currentOrder.getUser() == null ||
            !currentOrder.getUser().getIdUser().equals(idUser)
        ) {
            throw new RuntimeException(
                "Bạn không có quyền chỉnh sửa đơn hàng này."
            );
        }

        String productName =
                request.getProductName() == null
                    ? ""
                    : request.getProductName().trim();

        String unitType =
                request.getUnitType() == null
                    ? ""
                    : request.getUnitType().trim();

        BigDecimal quantity = request.getQuantity();

        if (productName.isBlank()) {
            throw new RuntimeException(
                "Tên sản phẩm không được để trống."
            );
        }

        if (unitType.isBlank()) {
            throw new RuntimeException(
                "Đơn vị tính không được để trống."
            );
        }

        if (
            quantity == null ||
            quantity.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new RuntimeException(
                "Số lượng phải lớn hơn 0."
            );
        }

        BigDecimal unitPrice =
                currentOrder.getUnitPrice() == null
                    ? BigDecimal.ZERO
                    : currentOrder.getUnitPrice();

        BigDecimal discountAmount =
                currentOrder.getDiscountAmount() == null
                    ? BigDecimal.ZERO
                    : currentOrder.getDiscountAmount();

        BigDecimal totalPrice = unitPrice
                .multiply(quantity)
                .subtract(discountAmount)
                .max(BigDecimal.ZERO);

        int affectedRows =
                repository.updateEditableFieldsByUser(
                    orderId,
                    idUser,
                    productName,
                    request.getCustomerRequest(),
                    unitType,
                    quantity,
                    totalPrice
                );

        if (affectedRows == 0) {
            throw new RuntimeException(
                "Không thể cập nhật đơn hàng."
            );
        }

        return repository
                .findById(orderId)
                .orElseThrow(
                    () -> new RuntimeException(
                        "Không tìm thấy đơn hàng sau khi cập nhật."
                    )
                );
    }
    
    @Override
    @Transactional
    public ServiceOrder updateAddressByUser(
            Long orderId,
            String idUser,
            UserUpdateOrderAddressRequest request
    ) {
        if (request == null || request.getAddressId() == null) {
            throw new RuntimeException(
                "Vui lòng chọn địa chỉ nhận hàng."
            );
        }

        ServiceOrder currentOrder = repository
            .findById(orderId)
            .orElseThrow(
                () -> new RuntimeException(
                    "Không tìm thấy đơn hàng."
                )
            );

        if (
            currentOrder.getDeletedAt() != null ||
            currentOrder.getUser() == null ||
            !idUser.equals(
                currentOrder.getUser().getIdUser()
            )
        ) {
            throw new RuntimeException(
                "Bạn không có quyền cập nhật địa chỉ của đơn hàng này."
            );
        }

        UserAddress selectedAddress = addressRepository
            .findByAddressIdAndUser_IdUserAndDeletedAtIsNull(
                request.getAddressId(),
                idUser
            )
            .orElseThrow(
                () -> new RuntimeException(
                    "Địa chỉ không tồn tại hoặc không thuộc người dùng hiện tại."
                )
            );

        int affectedRows = repository.updateAddressByUser(
            orderId,
            idUser,
            selectedAddress
        );

        if (affectedRows == 0) {
            throw new RuntimeException(
                "Không thể cập nhật địa chỉ đơn hàng."
            );
        }

        return repository
            .findById(orderId)
            .orElseThrow(
                () -> new RuntimeException(
                    "Không tìm thấy đơn hàng sau khi cập nhật địa chỉ."
                )
            );
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
    
    @Override
    public List<ServiceOrder> findByUserId(String idUser) {
        return repository.findByUser_IdUserAndDeletedAtIsNull(idUser);
    }
}
