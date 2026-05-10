package com.example.vaultr.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class KafkaPemConfig {

    @Value("${KAFKA_CA_PEM}")
    private String kafkaCaPem;

    @Value("${KAFKA_SVC_PEM}")
    private String kafkaSVCPem;

    @PostConstruct
    public void setupPemFiles() throws Exception {

        writeFile("/tmp/ca.pem", kafkaCaPem.replace("\\n", "\n"));
        writeFile("/tmp/svc.pem", kafkaSVCPem.replace("\\n", "\n"));
    }

    private void writeFile(String path, String content) throws Exception {
        Files.writeString(Path.of(path), content);
    }
}
