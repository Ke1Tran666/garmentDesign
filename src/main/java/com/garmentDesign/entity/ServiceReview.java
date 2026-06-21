package com.garmentDesign.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Service_Reviews")
public class ServiceReview {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "review_id")
	private Long reviewId;
    @ManyToOne @JoinColumn(name = "service_order_id") private ServiceOrder serviceOrder;
    @ManyToOne @JoinColumn(name = "user_id") private User user;
    private String reviewerName;
    private Integer rating;
    private String reviewContent;
    private String companyName;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    public ServiceReview() {}
    @PrePersist public void prePersist(){ createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate(){ updatedAt = LocalDateTime.now(); }
    
    public Long getReviewId(){return reviewId;} 
    public void setReviewId(Long reviewId){this.reviewId=reviewId;}
    
    public ServiceOrder getServiceOrder(){return serviceOrder;} 
    public void setServiceOrder(ServiceOrder serviceOrder){this.serviceOrder=serviceOrder;}
    
    public User getUser(){return user;} 
    public void setUser(User user){this.user=user;}
    
    public String getReviewerName(){return reviewerName;} 
    public void setReviewerName(String reviewerName){this.reviewerName=reviewerName;}
    
    public Integer getRating(){return rating;} 
    public void setRating(Integer rating){this.rating=rating;}
    
    public String getReviewContent(){return reviewContent;} 
    public void setReviewContent(String reviewContent){this.reviewContent=reviewContent;}
    
    public String getCompanyName(){return companyName;} 
    public void setCompanyName(String companyName){this.companyName=companyName;}
    
    public Boolean getIsPublic(){return isPublic;} 
    public void setIsPublic(Boolean isPublic){this.isPublic=isPublic;}
    
    public LocalDateTime getCreatedAt(){return createdAt;} 
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    
    public LocalDateTime getUpdatedAt(){return updatedAt;} 
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
    
    public LocalDateTime getDeletedAt(){return deletedAt;} 
    public void setDeletedAt(LocalDateTime deletedAt){this.deletedAt=deletedAt;}
}
