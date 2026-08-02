package com.garmentDesign.controller.rest;

import com.garmentDesign.dto.servicereview.ReviewableOrderResponse;
import com.garmentDesign.dto.servicereview.ServiceReviewRequest;
import com.garmentDesign.dto.servicereview.ServiceReviewResponse;
import com.garmentDesign.service.ServiceReviewService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-reviews")
@CrossOrigin(origins = "*")
public class ServiceReviewController {

    private final ServiceReviewService service;

    public ServiceReviewController(
            ServiceReviewService service
    ) {
        this.service = service;
    }

    @GetMapping("/public")
    public ResponseEntity<List<ServiceReviewResponse>>
            findPublicReviews() {
        return ResponseEntity.ok(
            service.findPublicReviews()
        );
    }

    @GetMapping("/user/{idUser}")
    public ResponseEntity<List<ServiceReviewResponse>>
            findByUser(
                @PathVariable("idUser")
                String idUser
            ) {
        return ResponseEntity.ok(
            service.findByUser(idUser)
        );
    }

    @GetMapping("/user/{idUser}/orders")
    public ResponseEntity<List<ReviewableOrderResponse>>
            findReviewableOrders(
                @PathVariable("idUser")
                String idUser
            ) {
        return ResponseEntity.ok(
            service.findReviewableOrders(idUser)
        );
    }

    @PostMapping("/order/{orderId}/user/{idUser}")
    public ResponseEntity<ServiceReviewResponse>
            createByUser(
                @PathVariable("orderId")
                Long orderId,

                @PathVariable("idUser")
                String idUser,

                @RequestBody
                ServiceReviewRequest request
            ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                service.createByUser(
                    orderId,
                    idUser,
                    request
                )
            );
    }

    @PutMapping("/{reviewId}/user/{idUser}")
    public ResponseEntity<ServiceReviewResponse>
            updateByUser(
                @PathVariable("reviewId")
                Long reviewId,

                @PathVariable("idUser")
                String idUser,

                @RequestBody
                ServiceReviewRequest request
            ) {
        return ResponseEntity.ok(
            service.updateByUser(
                reviewId,
                idUser,
                request
            )
        );
    }

    @DeleteMapping("/{reviewId}/user/{idUser}")
    public ResponseEntity<Void> deleteByUser(
            @PathVariable("reviewId")
            Long reviewId,

            @PathVariable("idUser")
            String idUser
    ) {
        service.deleteByUser(
            reviewId,
            idUser
        );

        return ResponseEntity.noContent().build();
    }
}