package com.garmentDesign.service;

import com.garmentDesign.entity.Service;
import java.util.List;

public interface ServiceService {
    List<Service> findAll();
    Service findById(Long id);
    Service save(Service data);
    Service update(Long id, Service data);
    void delete(Long id);
}
