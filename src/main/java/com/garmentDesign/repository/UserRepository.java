package com.garmentDesign.repository;

import com.garmentDesign.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
	Optional<User> findByUserCode(String userCode);

	boolean existsByUserCode(String userCode);
	
	Optional<User> findByIdUserAndDeletedAtIsNull(Long idUser);
}
