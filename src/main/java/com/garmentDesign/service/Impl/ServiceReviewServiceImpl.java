package com.garmentDesign.service.Impl;

import com.garmentDesign.dto.servicereview.ReviewableOrderResponse;
import com.garmentDesign.dto.servicereview.ServiceReviewRequest;
import com.garmentDesign.dto.servicereview.ServiceReviewResponse;
import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.entity.ServiceReview;
import com.garmentDesign.entity.User;
import com.garmentDesign.repository.ServiceOrderRepository;
import com.garmentDesign.repository.ServiceReviewRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.ServiceReviewService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceReviewServiceImpl
        implements ServiceReviewService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_COMPANY_LENGTH = 150;

    private final ServiceReviewRepository reviewRepository;
    private final ServiceOrderRepository orderRepository;
    private final UserRepository userRepository;

    public ServiceReviewServiceImpl(
            ServiceReviewRepository reviewRepository,
            ServiceOrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewableOrderResponse> findReviewableOrders(
            String idUser
    ) {
        validateUserId(idUser);

        List<ServiceOrder> orders =
                orderRepository.findReviewableOrdersByUser(
                    idUser,
                    LocalDate.now()
                );

        Map<Long, ServiceReview> reviewByOrderId =
                reviewRepository
                    .findByUser_IdUserAndDeletedAtIsNullOrderByCreatedAtDesc(
                        idUser
                    )
                    .stream()
                    .collect(
                        Collectors.toMap(
                            review ->
                                review
                                    .getServiceOrder()
                                    .getServiceOrderId(),
                            Function.identity(),
                            (first, ignored) -> first
                        )
                    );

        return orders
            .stream()
            .map(order -> new ReviewableOrderResponse(
                order.getServiceOrderId(),
                "ORD-" + order.getServiceOrderId(),
                order.getProductName(),
                order.getProductImage(),
                order.getService() == null
                    ? null
                    : order.getService().getServiceName(),
                order.getCompletedDate(),
                toResponse(
                    reviewByOrderId.get(
                        order.getServiceOrderId()
                    )
                )
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceReviewResponse> findByUser(
            String idUser
    ) {
        validateUserId(idUser);

        return reviewRepository
            .findByUser_IdUserAndDeletedAtIsNullOrderByCreatedAtDesc(
                idUser
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceReviewResponse> findPublicReviews() {
        return reviewRepository
            .findByIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public ServiceReviewResponse createByUser(
            Long orderId,
            String idUser,
            ServiceReviewRequest request
    ) {
        validateUserId(idUser);
        validateRequest(request);

        ServiceOrder order = orderRepository
            .findOwnedOrderForUpdate(
                orderId,
                idUser
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đơn hàng hoặc đơn không thuộc người dùng."
                )
            );

        validateReviewableOrder(order);

        boolean reviewExists =
                reviewRepository
                    .existsByServiceOrder_ServiceOrderIdAndUser_IdUserAndDeletedAtIsNull(
                        orderId,
                        idUser
                    );

        if (reviewExists) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Đơn hàng này đã được đánh giá."
            );
        }

        User user = userRepository
            .findById(idUser)
            .filter(item -> item.getDeletedAt() == null)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy người dùng."
                )
            );

        ServiceReview review = new ServiceReview();

        review.setServiceOrder(order);
        review.setUser(user);
        review.setReviewerName(resolveReviewerName(user));

        applyEditableFields(
            review,
            request
        );

        return toResponse(
            reviewRepository.save(review)
        );
    }

    @Override
    @Transactional
    public ServiceReviewResponse updateByUser(
            Long reviewId,
            String idUser,
            ServiceReviewRequest request
    ) {
        validateUserId(idUser);
        validateRequest(request);

        ServiceReview review = reviewRepository
            .findByReviewIdAndUser_IdUserAndDeletedAtIsNull(
                reviewId,
                idUser
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đánh giá hoặc bạn không có quyền chỉnh sửa."
                )
            );

        validateReviewableOrder(
            review.getServiceOrder()
        );

        applyEditableFields(
            review,
            request
        );

        return toResponse(
            reviewRepository.save(review)
        );
    }

    @Override
    @Transactional
    public void deleteByUser(
            Long reviewId,
            String idUser
    ) {
        validateUserId(idUser);

        ServiceReview review = reviewRepository
            .findByReviewIdAndUser_IdUserAndDeletedAtIsNull(
                reviewId,
                idUser
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đánh giá hoặc bạn không có quyền xóa."
                )
            );

        review.setDeletedAt(
            LocalDateTime.now()
        );

        reviewRepository.save(review);
    }

    private void validateReviewableOrder(
            ServiceOrder order
    ) {
        if (order.getDeletedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Không thể đánh giá đơn hàng đã bị hủy."
            );
        }

        LocalDate completedDate =
                order.getCompletedDate();

        if (
            completedDate == null ||
            completedDate.isAfter(LocalDate.now())
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Đơn hàng chưa hoàn tất nên chưa thể đánh giá."
            );
        }
    }

    private void validateRequest(
            ServiceReviewRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu đánh giá không hợp lệ."
            );
        }

        Integer rating = request.rating();

        if (
            rating == null ||
            rating < 1 ||
            rating > 5
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Số sao đánh giá phải từ 1 đến 5."
            );
        }

        String content = normalize(
            request.reviewContent()
        );

        if (content == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Nội dung đánh giá không được để trống."
            );
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Nội dung đánh giá không được vượt quá 2000 ký tự."
            );
        }

        String companyName = normalize(
            request.companyName()
        );

        if (
            companyName != null &&
            companyName.length() > MAX_COMPANY_LENGTH
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tên công ty không được vượt quá 150 ký tự."
            );
        }
    }

    private void applyEditableFields( ServiceReview review, ServiceReviewRequest request) {
        review.setRating(
            request.rating()
        );

        review.setReviewContent(
            normalize(request.reviewContent())
        );

        review.setCompanyName(
            normalize(request.companyName())
        );

        review.setIsPublic(
            request.isPublic() == null ||
            request.isPublic()
        );
    }

    private String resolveReviewerName(User user) {
        String fullName = normalize(
            user.getFullName()
        );

        if (fullName != null) {
            return fullName;
        }

        String userCode = normalize(
            user.getUserCode()
        );

        return userCode == null
            ? user.getIdUser()
            : userCode;
    }

    private void validateUserId(String idUser) {
        if (
            idUser == null ||
            idUser.isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Mã người dùng không hợp lệ."
            );
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private ServiceReviewResponse toResponse(ServiceReview review) {
        if (review == null) {
            return null;
        }

        ServiceOrder order =
                review.getServiceOrder();

        return new ServiceReviewResponse(
            review.getReviewId(),
            order.getServiceOrderId(),
            "ORD-" + order.getServiceOrderId(),
            order.getProductName(),
            order.getService() == null
                ? null
                : order.getService().getServiceName(),
            review.getReviewerName(),
            review.getCompanyName(),
            review.getRating(),
            review.getReviewContent(),
            Boolean.TRUE.equals(
                review.getIsPublic()
            ),
            order.getCompletedDate(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}