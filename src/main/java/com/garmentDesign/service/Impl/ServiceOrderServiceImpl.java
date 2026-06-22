package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.repository.ServiceOrderRepository;
import com.garmentDesign.service.ServiceOrderService;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {
    private final ServiceOrderRepository repository;

    public ServiceOrderServiceImpl(ServiceOrderRepository repository) {
        this.repository = repository;
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
    public void delete(Long id) {
        repository.deleteById(id);
    }
    
    @Override
    public List<ServiceOrder> findByUserId(String idUser) {
        return repository.findByUser_IdUserAndDeletedAtIsNull(idUser);
    }
}
