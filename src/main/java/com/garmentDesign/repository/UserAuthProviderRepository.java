package com.garmentDesign.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.garmentDesign.entity.UserAuthProvider;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {
	Optional<UserAuthProvider> findByEmailAndProvider(String email, String provider);
}
