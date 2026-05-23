package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.ServiceReview;
import com.garmentDesign.repository.ServiceReviewRepository;
import com.garmentDesign.service.ServiceReviewService;

@Service
public class ServiceReviewServiceImpl implements ServiceReviewService {
    private final ServiceReviewRepository repository;

    public ServiceReviewServiceImpl(ServiceReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceReview> findAll() {
        return repository.findAll();
    }

    @Override
    public ServiceReview findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
    }

    @Override
    public ServiceReview save(ServiceReview data) {
        return repository.save(data);
    }

    @Override
    public ServiceReview update(Long id, ServiceReview data) {
        ServiceReview oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
