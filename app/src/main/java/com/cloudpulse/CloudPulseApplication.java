package com.cloudpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CloudPulse — Cloud Infrastructure Monitoring API
 *
 * A production-grade REST API that monitors cloud resources,
 * tracks health status, and manages infrastructure incidents.
 */
@SpringBootApplication
@EnableScheduling
public class CloudPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPulseApplication.class, args);
    }
}
