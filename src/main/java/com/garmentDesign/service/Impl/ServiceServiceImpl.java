package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;

import com.garmentDesign.entity.Service;
import com.garmentDesign.repository.ServiceRepository;
import com.garmentDesign.service.ServiceService;

@org.springframework.stereotype.Service
public class ServiceServiceImpl implements ServiceService {
    private final ServiceRepository repository;

    public ServiceServiceImpl(ServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Service> findAll() {
        return repository.findAll();
    }

    @Override
    public Service findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
    }

    @Override
    public Service save(Service data) {
        return repository.save(data);
    }

    @Override
    public Service update(Long id, Service data) {
        Service oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
