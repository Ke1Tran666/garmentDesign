package com.garmentDesign.repository;

import com.garmentDesign.entity.ServiceOrder;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
