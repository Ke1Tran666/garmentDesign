package com.garmentDesign.controller.rest;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.garmentDesign.service.ServiceOrderAttachmentService;
import com.garmentDesign.service.ServiceOrderAttachmentService.DownloadFile;

@RestController
@RequestMapping("/api/service-order-files")
public class ServiceOrderFileController {

    private static final String X_CONTENT_TYPE_OPTIONS =
            "X-Content-Type-Options";

    private final ServiceOrderAttachmentService service;

    public ServiceOrderFileController(
            ServiceOrderAttachmentService service
    ) {
        this.service = service;
    }

    @PostMapping(
    	    value = "/me/orders/{orderId}",
    	    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    	)
    	public ResponseEntity<Map<String, Object>> uploadMine(
    	        @PathVariable Long orderId,
    	        Principal principal,

    	        @RequestParam(value = "image", required = false)
    	        MultipartFile image,

    	        @RequestParam(value = "files", required = false)
    	        MultipartFile[] files,

    	        @RequestParam(value = "note", required = false)
    	        String note
    	) {
    	    List<MultipartFile> fileList = files == null
    	            ? List.of()
    	            : Arrays.asList(files);

    	    Map<String, Object> response = service.upload(
    	            orderId,
    	            principal.getName(),
    	            image,
    	            fileList,
    	            note
    	        );

    	    return ResponseEntity.ok(response);
    	}

    @GetMapping("/me/orders/{orderId}")
    public ResponseEntity<List<Map<String, Object>>> findMyOrderFiles(
                @PathVariable Long orderId,
                Principal principal
            ) {
        return ResponseEntity.ok(
            service.findByOrder(
                orderId,
                principal.getName()
            )
        );
    }

    @GetMapping("/me/{fileId}/content")
    public ResponseEntity<Resource> getMyFileContent(
            @PathVariable Long fileId,
            Principal principal
    ) throws Exception {
        DownloadFile downloadFile = service.loadFile(
                fileId,
                principal.getName()
            );

        Resource resource = new UrlResource(downloadFile.path().toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File không tồn tại hoặc không thể đọc.");
        }

        MediaType mediaType = resolveMediaType(downloadFile.contentType());

        boolean isImage = "image".equalsIgnoreCase(
                mediaType.getType()
            );

        ContentDisposition disposition = createContentDisposition(
                isImage,
                downloadFile.fileName()
            );

        return ResponseEntity.ok()
            .contentType(mediaType)
            .contentLength(resource.contentLength())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                disposition.toString()
            )
            .header(
                X_CONTENT_TYPE_OPTIONS,
                "nosniff"
            )
            .body(resource);
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(
                contentType
            );
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private ContentDisposition createContentDisposition(
            boolean inline,String fileName
    ) {
        ContentDisposition.Builder builder =
                inline
                    ? ContentDisposition.inline()
                    : ContentDisposition.attachment();

        return builder
                .filename(
                    fileName,
                    StandardCharsets.UTF_8
                )
                .build();
    }
    
    @DeleteMapping("/me/{fileId}")
    public ResponseEntity<Void> deleteMyFile(
            @PathVariable Long fileId,
            Principal principal
    ) {
        service.deleteFileByUser(
            fileId,
            principal.getName()
        );

        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<List<Map<String, Object>>>
            findMine(
                Principal principal
            ) {
        return ResponseEntity.ok(
            service.findByUser(
                principal.getName()
            )
        );
    }
}