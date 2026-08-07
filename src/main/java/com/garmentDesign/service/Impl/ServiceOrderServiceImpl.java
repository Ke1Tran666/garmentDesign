package com.garmentDesign.service.Impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.garmentDesign.dto.serviceorder.UserRemoveServiceOrderResponse;
import com.garmentDesign.dto.serviceorder.UserUpdateOrderAddressRequest;
import com.garmentDesign.dto.serviceorder.UserUpdateServiceOrderRequest;
import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.entity.UserAddress;
import com.garmentDesign.repository.ServiceOrderRepository;
import com.garmentDesign.repository.ServiceReviewRepository;
import com.garmentDesign.repository.UserAddressRepository;
import com.garmentDesign.service.ServiceOrderAttachmentService;
import com.garmentDesign.service.ServiceOrderService;
import com.garmentDesign.dto.serviceorder.UserCreateServiceOrderRequest;
import com.garmentDesign.entity.User;
import com.garmentDesign.repository.ServiceRepository;
import com.garmentDesign.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {
	private final ServiceOrderRepository repository;
	private final UserAddressRepository addressRepository;
	
	private static final String CANCELLED_STATUS = "inactive";

	private final ServiceOrderAttachmentService attachmentService;
	private final ServiceReviewRepository reviewRepository;
	private final UserRepository userRepository;
	private final ServiceRepository serviceRepository;

	public ServiceOrderServiceImpl(
	        ServiceOrderRepository repository,
	        UserAddressRepository addressRepository,
	        UserRepository userRepository,
	        ServiceRepository serviceRepository,
	        ServiceOrderAttachmentService attachmentService,
	        ServiceReviewRepository reviewRepository
	) {
	    this.repository = repository;
	    this.addressRepository = addressRepository;
	    this.userRepository = userRepository;
	    this.serviceRepository = serviceRepository;
	    this.attachmentService = attachmentService;
	    this.reviewRepository = reviewRepository;
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
        	    currentOrder.getUnitType() == null
        	        ? ""
        	        : currentOrder.getUnitType().trim();

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
    @Transactional
    public UserRemoveServiceOrderResponse
            removeByUser(
                Long orderId,
                String idUser
            ) {
        if (
            orderId == null ||
            idUser == null ||
            idUser.isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Thông tin đơn hàng hoặc người dùng không hợp lệ."
            );
        }

        ServiceOrder currentOrder =
            repository
                .findOwnedOrderForUpdate(
                    orderId,
                    idUser
                )
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn hàng hoặc đơn không thuộc người dùng hiện tại."
                        )
                );

        if (currentOrder.getDeletedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Đơn hàng đã được hủy trước đó."
            );
        }

        boolean createdByIsEmpty =
            currentOrder.getCreatedBy() == null ||
            currentOrder
                .getCreatedBy()
                .isBlank();

        boolean updatedByIsEmpty =
            currentOrder.getUpdatedBy() == null ||
            currentOrder
                .getUpdatedBy()
                .isBlank();

        boolean hasNoReceiver =
            createdByIsEmpty &&
            updatedByIsEmpty;

        /*
         * Chưa có nhân viên nhận:
         * xóa vĩnh viễn.
         */
        if (hasNoReceiver) {
            Long deletedOrderId =
                currentOrder
                    .getServiceOrderId();

            attachmentService
                .deleteAllForPermanentOrder(
                    currentOrder
                );

            reviewRepository
                .deleteByServiceOrder_ServiceOrderId(
                    deletedOrderId
                );

            reviewRepository.flush();

            repository.delete(currentOrder);
            repository.flush();

            return new UserRemoveServiceOrderResponse(
                deletedOrderId,
                "DELETED",
                null,
                "Đơn hàng đã được xóa vĩnh viễn."
            );
        }

        /*
         * Đã có nhân viên nhận:
         * chỉ hủy đơn, vẫn giữ dữ liệu.
         */
        int affectedRows =
            repository
                .cancelAssignedOrderByUser(
                    orderId,
                    idUser,
                    CANCELLED_STATUS,
                    LocalDateTime.now()
                );

        if (affectedRows == 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Không thể hủy đơn hàng."
            );
        }

        ServiceOrder cancelledOrder =
            repository
                .findById(orderId)
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn hàng sau khi hủy."
                        )
                );

        return new UserRemoveServiceOrderResponse(
        	    orderId,
        	    "CANCELLED",
        	    cancelledOrder,
        	    "Đơn hàng đã được hủy."
        	);
    }
    
    @Override
    public List<ServiceOrder>findByUserId(String idUser) {
        return repository.findVisibleOrdersByUser(idUser);
    }
    
    @Override
    @Transactional
    public ServiceOrder createByUser(
            String idUser,
            UserCreateServiceOrderRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu đơn hàng không hợp lệ."
            );
        }

        String productName =
            request.getProductName() == null
                ? ""
                : request.getProductName().trim();

        if (productName.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tên sản phẩm không được để trống."
            );
        }

        if (
            request.getQuantity() == null ||
            request.getQuantity()
                .compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Số lượng phải lớn hơn 0."
            );
        }

        if (request.getServiceId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Vui lòng chọn dịch vụ."
            );
        }

        if (request.getAddressId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Vui lòng chọn địa chỉ nhận hàng."
            );
        }

        User currentUser = userRepository
            .findByIdUserAndDeletedAtIsNull(idUser)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy người dùng."
                )
            );

        var selectedService = serviceRepository
        	    .findById(request.getServiceId())
        	    .filter(item -> item.getDeletedAt() == null)
        	    .filter(item ->
        	        !"inactive".equalsIgnoreCase(
        	            item.getStatus() == null ? "" : item.getStatus().trim()
        	        )
        	    )
        	    .orElseThrow(() ->
        	        new ResponseStatusException(
        	            HttpStatus.NOT_FOUND,
        	            "Dịch vụ không tồn tại hoặc đã ngừng hoạt động."
        	        )
        	    );

        UserAddress selectedAddress = addressRepository
            .findByAddressIdAndUser_IdUserAndDeletedAtIsNull(
                request.getAddressId(),
                idUser
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Địa chỉ không tồn tại hoặc không thuộc người dùng."
                )
            );

        BigDecimal unitPrice =
            selectedService.getBasePrice() == null
                ? BigDecimal.ZERO
                : selectedService.getBasePrice();

        BigDecimal discountAmount = BigDecimal.ZERO;

        BigDecimal totalPrice = unitPrice
            .multiply(request.getQuantity())
            .subtract(discountAmount)
            .max(BigDecimal.ZERO);

        ServiceOrder order = new ServiceOrder();

        order.setUser(currentUser);
        order.setService(selectedService);
        order.setAddress(selectedAddress);
        order.setProductName(productName);
        order.setCustomerRequest(
            request.getCustomerRequest() == null
                ? null
                : request.getCustomerRequest().trim()
        );
        order.setUnitType(selectedService.getUnitType());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(unitPrice);
        order.setDiscountAmount(discountAmount);
        order.setTotalPrice(totalPrice);
        order.setStatus("pending");

        return repository.save(order);
    }
}
