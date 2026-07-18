package com.garmentDesign.service;

import com.garmentDesign.dto.serviceorder.UserUpdateOrderAddressRequest;
import com.garmentDesign.dto.serviceorder.UserUpdateServiceOrderRequest;
import com.garmentDesign.entity.ServiceOrder;
import java.util.List;

public interface ServiceOrderService {
    List<ServiceOrder> findAll();
    ServiceOrder findById(Long id);
    ServiceOrder save(ServiceOrder data);
    ServiceOrder update(Long id, ServiceOrder data);
    ServiceOrder updateByUser(
    	    Long orderId,
    	    String idUser,
    	    UserUpdateServiceOrderRequest request
    	);
    void delete(Long id);
    List<ServiceOrder> findByUserId(String idUser);
    
    ServiceOrder updateAddressByUser(
    	    Long orderId,
    	    String idUser,
    	    UserUpdateOrderAddressRequest request
    	);
}
