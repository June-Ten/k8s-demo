package com.demo.k8s.status;

public record StatusResponse(String app, String mysql, String redis, String pod) {
}
