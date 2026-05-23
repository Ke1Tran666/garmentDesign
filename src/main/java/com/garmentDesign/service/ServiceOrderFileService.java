package com.garmentDesign.service;

import com.garmentDesign.entity.ServiceOrderFile;
import java.util.List;

public interface ServiceOrderFileService {
    List<ServiceOrderFile> findAll();
    ServiceOrderFile findById(Long id);
    ServiceOrderFile save(ServiceOrderFile data);
    ServiceOrderFile update(Long id, ServiceOrderFile data);
    void delete(Long id);
}
