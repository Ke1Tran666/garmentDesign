package com.garmentDesign.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.garmentDesign.entity.UserAuthProvider;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

	Optional<UserAuthProvider> findByEmailAndProvider(String email, String provider);

	Optional<UserAuthProvider> findByEmailAndProviderAndDeletedAtIsNull(String email, String provider);

	Optional<UserAuthProvider> findByPhoneAndProvider(String phone, String provider);

	Optional<UserAuthProvider> findByProviderIdAndProvider(String providerId, String provider);

	List<UserAuthProvider> findByUser_IdUserAndDeletedAtIsNull(String idUser);
	
	List<UserAuthProvider> findByUser_IdUserOrderByCreatedAtAsc(
			String idUser);

	long countByUser_IdUserAndDeletedAtIsNull(String idUser);

	Optional<UserAuthProvider> findByUser_IdUserAndProviderAndDeletedAtIsNull(String idUser, String provider);

	List<UserAuthProvider> findAllByEmailIgnoreCase(String email);

	Optional<UserAuthProvider> findByIdAndUser_IdUserAndProviderAndDeletedAtIsNull(Long id, String idUser,
			String provider);
}