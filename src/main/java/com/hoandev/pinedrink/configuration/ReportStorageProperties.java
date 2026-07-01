package com.hoandev.pinedrink.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.report.storage")
public class ReportStorageProperties {
    private String localDir = "./storage/reports";

    @Bean
    public Path reportStorageRootPath() throws IOException {
        Path path = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }
}
