package com.garmentDesign.service.Impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.garmentDesign.entity.ServiceOrderFile;
import com.garmentDesign.repository.ServiceOrderFileRepository;
import com.garmentDesign.service.ServiceOrderFileService;

@Service
public class ServiceOrderFileServiceImpl implements ServiceOrderFileService {
    private final ServiceOrderFileRepository repository;

    public ServiceOrderFileServiceImpl(ServiceOrderFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceOrderFile> findAll() {
        return repository.findAll();
    }

    @Override
    public ServiceOrderFile findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu với id: " + id));
    }

    @Override
    public ServiceOrderFile save(ServiceOrderFile data) {
        return repository.save(data);
    }

    @Override
    public ServiceOrderFile update(Long id, ServiceOrderFile data) {
        ServiceOrderFile oldData = findById(id);
        BeanUtils.copyProperties(data, oldData);
        return repository.save(oldData);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
