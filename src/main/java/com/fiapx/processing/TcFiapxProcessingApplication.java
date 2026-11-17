package com.fiapx.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TcFiapxProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcFiapxProcessingApplication.class, args);
    }
}
