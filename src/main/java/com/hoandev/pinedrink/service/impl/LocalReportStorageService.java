package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.ReportStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Nhận byte[] từ JasperReportServiceImpl
 → lưu thành file thật trong thư mục local
 → trả về đường dẫn tương đối
 → khi cần thì load lại file để tải xuống/xem
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalReportStorageService implements ReportStorageService {

    private final Path rootDir;

    @Override
    public String save(byte[] bytes, String relativeFolder, String filename) {
        try {
            Path folder = resolveSafe(relativeFolder);
            Files.createDirectories(folder);

            Path target = folder.resolve(filename).normalize();
            if (!target.startsWith(rootDir)) {
                throw new BaseException(ErrorCode.COM_004, "Invalid report path");
            }

            Files.write(target, bytes);
            return rootDir.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            log.error("Failed to save report file", e);
            throw new BaseException(ErrorCode.COM_002, "Failed to save report file");
        }
    }

    @Override
    public Resource load(String relativePath) {
        try {
            Path file = resolve(relativePath);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BaseException(ErrorCode.COM_005, "Report file not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BaseException(ErrorCode.COM_004, "Invalid report path");
        }
    }

    @Override
    public Path resolve(String relativePath) {
        return resolveSafe(relativePath);
    }

    private Path resolveSafe(String relativePath) {
        Path path = rootDir.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!path.startsWith(rootDir)) {
            throw new BaseException(ErrorCode.COM_004, "Invalid report path");
        }
        return path;
    }
}
