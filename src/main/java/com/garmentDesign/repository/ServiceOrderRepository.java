package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceOrder;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
	List<ServiceOrder> findByUser_IdUserAndDeletedAtIsNull(String idUser);
}
