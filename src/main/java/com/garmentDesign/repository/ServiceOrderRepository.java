package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.entity.UserAddress;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
	List<ServiceOrder> findByUser_IdUserAndDeletedAtIsNull(String idUser);
	
	 @Modifying(
		        clearAutomatically = true,
		        flushAutomatically = true
		    )
		    @Query("""
		        UPDATE ServiceOrder serviceOrder
		        SET serviceOrder.productName = :productName,
		            serviceOrder.customerRequest = :customerRequest,
		            serviceOrder.unitType = :unitType,
		            serviceOrder.quantity = :quantity,
		            serviceOrder.totalPrice = :totalPrice
		        WHERE serviceOrder.serviceOrderId = :orderId
		          AND serviceOrder.user.idUser = :idUser
		          AND serviceOrder.deletedAt IS NULL
		    """)
		    int updateEditableFieldsByUser(
		        @Param("orderId") Long orderId,
		        @Param("idUser") String idUser,
		        @Param("productName") String productName,
		        @Param("customerRequest") String customerRequest,
		        @Param("unitType") String unitType,
		        @Param("quantity") BigDecimal quantity,
		        @Param("totalPrice") BigDecimal totalPrice
		    );
	 
	 @Modifying(
			    clearAutomatically = true,
			    flushAutomatically = true
			)
			@Query("""
			    UPDATE ServiceOrder serviceOrder
			    SET serviceOrder.productImage = :productImage
			    WHERE serviceOrder.serviceOrderId = :orderId
			      AND serviceOrder.user.idUser = :idUser
			      AND serviceOrder.deletedAt IS NULL
			""")
			int updateProductImageByUser(
			    @Param("orderId") Long orderId,
			    @Param("idUser") String idUser,
			    @Param("productImage") String productImage
			);
	 
	 @Modifying(
			    clearAutomatically = true,
			    flushAutomatically = true
			)
			@Query("""
			    UPDATE ServiceOrder serviceOrder
			    SET serviceOrder.address = :address
			    WHERE serviceOrder.serviceOrderId = :orderId
			      AND serviceOrder.user.idUser = :idUser
			      AND serviceOrder.deletedAt IS NULL
			""")
			int updateAddressByUser(
			    @Param("orderId") Long orderId,
			    @Param("idUser") String idUser,
			    @Param("address") UserAddress address
			);
	 
	 @Lock(LockModeType.PESSIMISTIC_WRITE)
	 @Query("""
	     SELECT serviceOrder
	     FROM ServiceOrder serviceOrder
	     WHERE serviceOrder.serviceOrderId = :orderId
	       AND serviceOrder.user.idUser = :idUser
	 """)
	 Optional<ServiceOrder>
	 findOwnedOrderForUpdate(
	     @Param("orderId")
	     Long orderId,

	     @Param("idUser")
	     String idUser
	 );
	 
	 @Modifying(
			    clearAutomatically = true,
			    flushAutomatically = true
			)
			@Query("""
			    UPDATE ServiceOrder serviceOrder
			    SET serviceOrder.status = :status,
			        serviceOrder.deletedAt = :deletedAt
			    WHERE serviceOrder.serviceOrderId = :orderId
			      AND serviceOrder.user.idUser = :idUser
			      AND serviceOrder.deletedAt IS NULL
			      AND (
			          (
			              serviceOrder.createdBy IS NOT NULL
			              AND TRIM(serviceOrder.createdBy) <> ''
			          )
			          OR
			          (
			              serviceOrder.updatedBy IS NOT NULL
			              AND TRIM(serviceOrder.updatedBy) <> ''
			          )
			      )
			""")
			int cancelAssignedOrderByUser(
			    @Param("orderId")
			    Long orderId,

			    @Param("idUser")
			    String idUser,

			    @Param("status")
			    String status,

			    @Param("deletedAt")
			    LocalDateTime deletedAt
			);
	 
	 @Query("""
			    SELECT serviceOrder
			    FROM ServiceOrder serviceOrder
			    WHERE serviceOrder.user.idUser = :idUser
			      AND (
			          serviceOrder.deletedAt IS NULL
			          OR (
			              serviceOrder.deletedAt IS NOT NULL
			              AND LOWER(serviceOrder.status) = 'inactive'
			          )
			      )
			    ORDER BY serviceOrder.serviceOrderId DESC
			""")
			List<ServiceOrder>
			findVisibleOrdersByUser(
			    @Param("idUser")
			    String idUser
			);
	 
	 @Query("""
			    SELECT serviceOrder
			    FROM ServiceOrder serviceOrder
			    LEFT JOIN FETCH serviceOrder.service
			    WHERE serviceOrder.user.idUser = :idUser
			      AND serviceOrder.completedDate IS NOT NULL
			      AND serviceOrder.completedDate <= :currentDate
			      AND serviceOrder.deletedAt IS NULL
			    ORDER BY serviceOrder.completedDate DESC,
			             serviceOrder.serviceOrderId DESC
			""")
			List<ServiceOrder> findReviewableOrdersByUser(
			    @Param("idUser") String idUser,
			    @Param("currentDate") java.time.LocalDate currentDate
			);
}
