package com.visualpathit.account.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Health check controller for Kubernetes liveness/readiness probes.
 */
@Controller
public class HealthController {

    @GetMapping("/health")
    @ResponseBody
    public String healthCheck() {
        return "{\"status\":\"UP\",\"service\":\"vprofile\"}";
    }

    @GetMapping("/")
    @ResponseBody
    public String index() {
        return "vProfile Application — DevSecOps Pipeline";
    }
}
