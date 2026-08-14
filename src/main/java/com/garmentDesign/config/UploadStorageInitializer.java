package com.garmentDesign.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadStorageInitializer implements InitializingBean {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UploadStorageInitializer.class);

    private final Path uploadRoot;

    public UploadStorageInitializer(
            @Value("${app.upload.root-dir:uploads}") String uploadRoot) {

        if (uploadRoot == null || uploadRoot.isBlank()) {
            throw new IllegalStateException(
                    "UPLOAD_ROOT không được để trống");
        }

        this.uploadRoot = Path.of(uploadRoot)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public void afterPropertiesSet() {
        Path writeProbe = null;

        try {
            // Tự tạo /home/uploads nếu chưa có
            Files.createDirectories(uploadRoot);

            // Kiểm tra App Service có quyền ghi hay không
            writeProbe = Files.createTempFile(
                    uploadRoot,
                    ".write-test-",
                    ".tmp");

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Không thể sử dụng thư mục upload "
                            + uploadRoot
                            + ". Hãy kiểm tra UPLOAD_ROOT và quyền ghi.",
                    exception);

        } finally {
            if (writeProbe != null) {
                try {
                    Files.deleteIfExists(writeProbe);
                } catch (IOException exception) {
                    LOGGER.warn(
                            "Không thể xóa file kiểm tra {}",
                            writeProbe,
                            exception);
                }
            }
        }

        LOGGER.info(
                "Upload storage đã sẵn sàng tại {}",
                uploadRoot);
    }
}