package com.garmentDesign.service;

import com.garmentDesign.entity.ServiceOrder;
import com.garmentDesign.entity.ServiceOrderFile;
import com.garmentDesign.repository.ServiceOrderFileRepository;
import com.garmentDesign.repository.ServiceOrderRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServiceOrderAttachmentService {

    private static final long MAX_FILE_SIZE =
            50L * 1024L * 1024L;

    private final ServiceOrderRepository orderRepository;
    private final ServiceOrderFileRepository fileRepository;
    private final Path uploadRoot;

    public ServiceOrderAttachmentService(
            ServiceOrderRepository orderRepository,
            ServiceOrderFileRepository fileRepository,
            @Value("${app.upload.service-order-dir}")
            String uploadDirectory
    ) {
        this.orderRepository = orderRepository;
        this.fileRepository = fileRepository;

        this.uploadRoot = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    @Transactional
    public Map<String, Object> upload(
            Long orderId,
            String idUser,
            MultipartFile image,
            List<MultipartFile> files,
            String note
    ) {
        ServiceOrder order = orderRepository
                .findById(orderId)
                .orElseThrow(
                    () -> new RuntimeException(
                        "Không tìm thấy đơn hàng."
                    )
                );

        validateOwner(order, idUser);

        String userCode =
                order.getUser().getUserCode();

        if (
            userCode == null ||
            userCode.isBlank()
        ) {
            userCode = idUser;
        }

        String safeUserCode =
                sanitizeFolderName(userCode);

        String orderFolderName =
                safeUserCode
                + "_"
                + orderId;

        /*
         * uploads/service-orders/{userCode}
         */
        Path userDirectory = uploadRoot
                .resolve(safeUserCode)
                .normalize();

        /*
         * uploads/service-orders/{userCode}/{userCode}_{orderId}
         */
        Path orderDirectory = userDirectory
                .resolve(orderFolderName)
                .normalize();

        validatePath(userDirectory);
        validatePath(orderDirectory);

        /*
         * Path tương đối được lưu vào database.
         * Ví dụ: USR001/USR001_15
         */
        String relativeOrderDirectory =
                safeUserCode
                + "/"
                + orderFolderName;

        try {
            Files.createDirectories(orderDirectory);
        } catch (Exception exception) {
            throw new RuntimeException(
                "Không thể tạo thư mục đơn hàng.",
                exception
            );
        }

        List<ServiceOrderFile> uploadedFiles =
                new ArrayList<>();

        /*
         * Ảnh sản phẩm
         */
        if (image != null && !image.isEmpty()) {
            String oldProductImage =
                    order.getProductImage();

            String productImageUrl =
                    saveProductImage(
                        image,
                        orderDirectory,
                        relativeOrderDirectory
                    );

            int affectedRows =
                    orderRepository
                        .updateProductImageByUser(
                            orderId,
                            idUser,
                            productImageUrl
                        );

            if (affectedRows == 0) {
                deleteProductImage(productImageUrl);

                throw new RuntimeException(
                    "Không thể cập nhật ảnh đại diện."
                );
            }

            /*
             * Chỉ xóa ảnh đại diện cũ sau khi database
             * đã cập nhật ảnh mới thành công.
             */
            deleteProductImage(oldProductImage);
        }

        /*
         * File đính kèm: nhận mọi định dạng
         */
        if (files != null) {
            for (MultipartFile file : files) {
                if (
                    file == null ||
                    file.isEmpty()
                ) {
                    continue;
                }

                ServiceOrderFile savedFile = saveFile(
                	    file,
                	    order,
                	    idUser,
                	    orderDirectory,
                	    relativeOrderDirectory,
                	    note
                	);

                uploadedFiles.add(savedFile);
            }
        }

        ServiceOrder updatedOrder = orderRepository
                .findById(orderId)
                .orElseThrow(
                    () -> new RuntimeException(
                        "Không tìm thấy đơn hàng sau khi upload."
                    )
                );

        Map<String, Object> response =
                new HashMap<>();

        response.put("message", "Upload file thành công");
        response.put("order", updatedOrder);
        response.put(
            "files",
            uploadedFiles.stream()
                .map(this::toResponse)
                .toList()
        );

        return response;
    }

    public List<Map<String, Object>> findByOrder(
            Long orderId
    ) {
        return fileRepository
                .findByServiceOrder_ServiceOrderId(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DownloadFile loadFile(Long fileId) {
        ServiceOrderFile file = fileRepository
                .findById(fileId)
                .orElseThrow(
                    () -> new RuntimeException(
                        "Không tìm thấy file."
                    )
                );

        Path filePath = uploadRoot
                .resolve(file.getFilePath())
                .normalize();

        validatePath(filePath);

        if (!Files.exists(filePath)) {
            throw new RuntimeException(
                "File không tồn tại trên hệ thống."
            );
        }

        return new DownloadFile(
            filePath,
            file.getFileName(),
            file.getFileType()
        );
    }

    private ServiceOrderFile saveFile(
            MultipartFile multipartFile,
            ServiceOrder order,
            String idUser,
            Path orderDirectory,
            String relativeOrderDirectory,
            String note
    ) {
        validateFile(multipartFile);

        String originalName = sanitizeFileName(
            multipartFile.getOriginalFilename()
        );

        String extension = getExtension(originalName);

        String storedFileName =
                UUID.randomUUID()
                + extension;

        Path targetPath = orderDirectory
                .resolve(storedFileName)
                .normalize();

        validatePath(targetPath);

        try (
            InputStream inputStream =
                    multipartFile.getInputStream()
        ) {
            Files.copy(
                inputStream,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception exception) {
            throw new RuntimeException(
                "Không thể lưu file: " + originalName,
                exception
            );
        }

        ServiceOrderFile file =
                new ServiceOrderFile();

        file.setServiceOrder(order);
        file.setFileName(originalName);
        file.setFileType(
            Objects.requireNonNullElse(
                multipartFile.getContentType(),
                "application/octet-stream"
            )
        );

        /*
         * Lưu path tương đối so với uploadRoot.
         */
        file.setFilePath(
        	    relativeOrderDirectory
        	    + "/"
        	    + storedFileName
        	);

        file.setUploadedBy(idUser);
        file.setNote(note);

        return fileRepository.save(file);
    }

    private void validateOwner(
            ServiceOrder order,
            String idUser
    ) {
        if (
            order.getUser() == null ||
            !Objects.equals(
                order.getUser().getIdUser(),
                idUser
            )
        ) {
            throw new RuntimeException(
                "Bạn không có quyền cập nhật đơn hàng này."
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException(
                "File không được để trống."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException(
                "Mỗi file không được vượt quá 50MB."
            );
        }
    }

    private String sanitizeFolderName(String value) {
        return value.replaceAll(
            "[^a-zA-Z0-9_-]",
            "_"
        );
    }

    private String sanitizeFileName(String fileName) {
        String cleanedName = StringUtils.cleanPath(
            Objects.requireNonNullElse(
                fileName,
                "file"
            )
        );

        String safeName = Paths.get(cleanedName)
                .getFileName()
                .toString();

        if (safeName.contains("..")) {
            throw new RuntimeException(
                "Tên file không hợp lệ."
            );
        }

        return safeName;
    }

    private String getExtension(String fileName) {
        int extensionIndex =
                fileName.lastIndexOf(".");

        if (
            extensionIndex < 0 ||
            extensionIndex == fileName.length() - 1
        ) {
            return "";
        }

        String extension =
                fileName.substring(extensionIndex);

        /*
         * Tránh extension bất thường quá dài.
         */
        return extension.length() <= 15
                ? extension
                : "";
    }

    private void validatePath(Path path) {
        if (!path.startsWith(uploadRoot)) {
            throw new RuntimeException(
                "Đường dẫn lưu file không hợp lệ."
            );
        }
    }

    private Map<String, Object> toResponse(
            ServiceOrderFile file
    ) {
        Map<String, Object> response =
                new HashMap<>();

        response.put("fileId", file.getFileId());
        response.put("fileName", file.getFileName());
        response.put("fileType", file.getFileType());
        response.put("filePath", file.getFilePath());
        response.put("uploadedBy", file.getUploadedBy());
        response.put("uploadedAt", file.getUploadedAt());
        response.put("note", file.getNote());

        response.put(
            "contentUrl",
            "/api/service-order-files/"
            + file.getFileId()
            + "/content"
        );

        return response;
    }

    public record DownloadFile(
        Path path,
        String fileName,
        String contentType
    ) {}
    
    private String saveProductImage(
            MultipartFile image,
            Path orderDirectory,
            String relativeOrderDirectory
    ) {
        validateFile(image);

        String contentType = image.getContentType();

        if (
            contentType == null ||
            !contentType.startsWith("image/")
        ) {
            throw new RuntimeException(
                "File ảnh không đúng định dạng."
            );
        }

        String originalName = sanitizeFileName(
            image.getOriginalFilename()
        );

        String extension =
                getExtension(originalName)
                    .toLowerCase();

        String storedFileName =
                "product-image-"
                + UUID.randomUUID()
                + extension;

        Path targetPath = orderDirectory
                .resolve(storedFileName)
                .normalize();

        validatePath(targetPath);

        try (
            InputStream inputStream =
                    image.getInputStream()
        ) {
            Files.copy(
                inputStream,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception exception) {
            throw new RuntimeException(
                "Không thể lưu ảnh đại diện.",
                exception
            );
        }

        /*
         * Ví dụ:
         * /uploads/service-orders/USR001/USR001_15/product-image-uuid.jpg
         */
        return "/uploads/service-orders/"
                + relativeOrderDirectory
                + "/"
                + storedFileName;
    }
    
    private void deleteProductImage(
            String productImageUrl
    ) {
        if (
            productImageUrl == null ||
            productImageUrl.isBlank()
        ) {
            return;
        }

        String urlPrefix =
                "/uploads/service-orders/";

        /*
         * Không xóa ảnh ngoài thư mục quản lý.
         * Đồng thời bỏ qua URL cũ sử dụng fileId.
         */
        if (!productImageUrl.startsWith(urlPrefix)) {
            return;
        }

        String relativePath =
                productImageUrl.substring(
                    urlPrefix.length()
                );

        Path imagePath = uploadRoot
                .resolve(relativePath)
                .normalize();

        validatePath(imagePath);

        try {
            Files.deleteIfExists(imagePath);
        } catch (Exception exception) {
            /*
             * Không hủy transaction chỉ vì không xóa
             * được ảnh cũ.
             */
            System.err.println(
                "Không thể xóa ảnh đại diện cũ: "
                + imagePath
            );
        }
    }
    
    @Transactional
    public void deleteFileByUser(
            Long fileId,
            String idUser
    ) {
        ServiceOrderFile file =
                fileRepository.findById(fileId)
                    .orElseThrow(
                        () -> new RuntimeException(
                            "Không tìm thấy file."
                        )
                    );

        ServiceOrder order =
                file.getServiceOrder();

        validateOwner(order, idUser);

        Path filePath = uploadRoot
                .resolve(file.getFilePath())
                .normalize();

        validatePath(filePath);

        /*
         * Xóa record và flush trước.
         * Nếu xóa file vật lý thất bại,
         * transaction database sẽ rollback.
         */
        fileRepository.delete(file);
        fileRepository.flush();

        try {
            Files.deleteIfExists(filePath);
        } catch (Exception exception) {
            throw new RuntimeException(
                "Không thể xóa file khỏi hệ thống.",
                exception
            );
        }
    }
}