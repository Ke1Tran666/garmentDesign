package com.garmentDesign.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.garmentDesign.entity.UserAddress;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser_IdUserAndDeletedAtIsNull(String idUser);
    Optional<UserAddress> findByUser_IdUserAndCompanyNameIgnoreCaseAndAddressIgnoreCase(
    	    String idUser,
    	    String companyName,
    	    String address
    	);

}