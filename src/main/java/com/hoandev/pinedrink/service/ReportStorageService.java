package com.hoandev.pinedrink.service;

import org.springframework.core.io.Resource;

import java.nio.file.Path;

/**
 * Abstraction cho việc lưu trữ file báo cáo đã sinh.
 * <p>
 * Implementation quyết định nơi lưu nội dung file, ví dụ ổ đĩa local hoặc
 * kho lưu trữ đối tượng. Bên gọi chỉ nên lưu đường dẫn tương đối được trả về vào cơ sở dữ liệu
 * và dùng lại service này khi cần tải file để download.
 */
public interface ReportStorageService {

    /**
     * Lưu nội dung báo cáo vào một thư mục bên dưới thư mục gốc lưu trữ báo cáo.
     *
     * @param bytes nội dung nhị phân của báo cáo đã sinh
     * @param relativeFolder thư mục tương đối so với thư mục gốc lưu trữ báo cáo
     * @param filename tên file cần ghi bên trong {@code relativeFolder}
     * @return đường dẫn tương đối có thể lưu vào cơ sở dữ liệu và truyền lại cho {@link #load(String)}
     */
    String save(byte[] bytes, String relativeFolder, String filename);

    /**
     * Tải một file báo cáo đã lưu dưới dạng tài nguyên Spring.
     *
     * @param relativePath path được trả về bởi {@link #save(byte[], String, String)}
     * @return tài nguyên có thể đọc của file báo cáo đã lưu
     */
    Resource load(String relativePath);

    /**
     * Chuyển path báo cáo đã lưu thành path trên filesystem.
     * <p>
     * Implementation phải ngăn path traversal ra ngoài thư mục gốc lưu trữ báo cáo.
     *
     * @param relativePath path tương đối so với thư mục gốc lưu trữ báo cáo
     * @return filesystem path đã được normalize
     */
    Path resolve(String relativePath);
}
