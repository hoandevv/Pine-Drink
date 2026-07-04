package com.hoandev.pinedrink.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.report.storage")
public class ReportStorageProperties {
    private String localDir = "./storage/reports";
    @NotNull
    private Duration runningTimeout;

    @Bean
    public Path reportStorageRootPath() throws IOException {
        Path path = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }
}
