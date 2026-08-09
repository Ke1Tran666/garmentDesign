package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceOrderFile;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOrderFileRepository extends JpaRepository<ServiceOrderFile, Long> {
	List<ServiceOrderFile> findByServiceOrder_ServiceOrderId(Long serviceOrderId);

	void deleteByServiceOrder_ServiceOrderId(Long serviceOrderId);

	List<ServiceOrderFile> findByServiceOrder_User_IdUser(String idUser);
}
