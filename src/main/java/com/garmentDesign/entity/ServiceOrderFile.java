package com.garmentDesign.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Service_Order_Files")
public class ServiceOrderFile {
    @Id
    private Long fileId;
    @ManyToOne @JoinColumn(name = "service_order_id") private ServiceOrder serviceOrder;
    private String fileType;
    private String fileName;
    private String filePath;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String note;
    
    public ServiceOrderFile() {}
    @PrePersist public void prePersist(){ uploadedAt = LocalDateTime.now(); }
    public Long getFileId(){return fileId;} 
    public void setFileId(Long fileId){this.fileId=fileId;}
    
    public ServiceOrder getServiceOrder(){return serviceOrder;} 
    public void setServiceOrder(ServiceOrder serviceOrder){this.serviceOrder=serviceOrder;}
    
    public String getFileType(){return fileType;} 
    public void setFileType(String fileType){this.fileType=fileType;}
    
    public String getFileName(){return fileName;} 
    public void setFileName(String fileName){this.fileName=fileName;}
    
    public String getFilePath(){return filePath;} 
    public void setFilePath(String filePath){this.filePath=filePath;}
    
    public String getUploadedBy(){return uploadedBy;} 
    public void setUploadedBy(String uploadedBy){this.uploadedBy=uploadedBy;}
    
    public LocalDateTime getUploadedAt(){return uploadedAt;} 
    public void setUploadedAt(LocalDateTime uploadedAt){this.uploadedAt=uploadedAt;}
    
    public String getNote(){return note;} 
    public void setNote(String note){this.note=note;}
}
