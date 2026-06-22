package com.garmentDesign.service;

import com.garmentDesign.entity.ServiceOrder;
import java.util.List;

public interface ServiceOrderService {
    List<ServiceOrder> findAll();
    ServiceOrder findById(Long id);
    ServiceOrder save(ServiceOrder data);
    ServiceOrder update(Long id, ServiceOrder data);
    void delete(Long id);
    List<ServiceOrder> findByUserId(String idUser);
}
