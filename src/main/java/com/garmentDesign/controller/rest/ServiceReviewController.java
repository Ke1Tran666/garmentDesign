package com.garmentDesign.controller.rest;

import com.garmentDesign.dto.servicereview.ReviewableOrderResponse;
import com.garmentDesign.dto.servicereview.ServiceReviewRequest;
import com.garmentDesign.dto.servicereview.ServiceReviewResponse;
import com.garmentDesign.service.ServiceReviewService;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-reviews")
public class ServiceReviewController {

    private final ServiceReviewService service;

    public ServiceReviewController(
            ServiceReviewService service
    ) {
        this.service = service;
    }

    /*
     * Nội dung công khai, không cần đăng nhập.
     */
    @GetMapping("/public")
    public ResponseEntity<List<ServiceReviewResponse>>
            findPublicReviews() {
        return ResponseEntity.ok(
            service.findPublicReviews()
        );
    }

    /*
     * Danh sách đánh giá của người đang đăng nhập.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ServiceReviewResponse>>
            findMine(
                Principal principal
            ) {
        return ResponseEntity.ok(
            service.findByUser(
                principal.getName()
            )
        );
    }

    /*
     * Các đơn hàng người hiện tại có thể đánh giá.
     */
    @GetMapping("/me/orders")
    public ResponseEntity<List<ReviewableOrderResponse>>
            findMyReviewableOrders(
                Principal principal
            ) {
        return ResponseEntity.ok(
            service.findReviewableOrders(
                principal.getName()
            )
        );
    }

    /*
     * Tạo đánh giá cho đơn hàng thuộc người hiện tại.
     */
    @PostMapping("/me/orders/{orderId}")
    public ResponseEntity<ServiceReviewResponse>
            createMine(
                @PathVariable Long orderId,
                Principal principal,
                @RequestBody
                ServiceReviewRequest request
            ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                service.createByUser(
                    orderId,
                    principal.getName(),
                    request
                )
            );
    }

    /*
     * Cập nhật đánh giá thuộc người hiện tại.
     */
    @PutMapping("/me/{reviewId}")
    public ResponseEntity<ServiceReviewResponse>
            updateMine(
                @PathVariable Long reviewId,
                Principal principal,
                @RequestBody
                ServiceReviewRequest request
            ) {
        return ResponseEntity.ok(
            service.updateByUser(
                reviewId,
                principal.getName(),
                request
            )
        );
    }

    /*
     * Xóa mềm đánh giá thuộc người hiện tại.
     */
    @DeleteMapping("/me/{reviewId}")
    public ResponseEntity<Void> deleteMine(
            @PathVariable Long reviewId,
            Principal principal
    ) {
        service.deleteByUser(
            reviewId,
            principal.getName()
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}