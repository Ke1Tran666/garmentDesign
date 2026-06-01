package com.garmentDesign.repository;

import com.garmentDesign.entity.UserAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser_IdUserAndDeletedAtIsNull(String idUser);

}