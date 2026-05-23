package com.garmentDesign.service;

import com.garmentDesign.entity.ServiceReview;
import java.util.List;

public interface ServiceReviewService {
    List<ServiceReview> findAll();
    ServiceReview findById(Long id);
    ServiceReview save(ServiceReview data);
    ServiceReview update(Long id, ServiceReview data);
    void delete(Long id);
}
