package com.demo.k8s.status;

import java.net.InetAddress;

import com.demo.k8s.note.NoteRepository;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final NoteRepository repository;
    private final StringRedisTemplate redis;

    public StatusController(NoteRepository repository, StringRedisTemplate redis) {
        this.repository = repository;
        this.redis = redis;
    }

    @GetMapping
    public StatusResponse status() {
        return new StatusResponse("k8s-demo", mysqlStatus(), redisStatus(), podName());
    }

    private String mysqlStatus() {
        try {
            repository.count();
            return "UP";
        } catch (RuntimeException ex) {
            return "DOWN";
        }
    }

    private String redisStatus() {
        try {
            String pong = redis.execute(RedisConnection::ping);
            return pong != null && "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (RuntimeException ex) {
            return "DOWN";
        }
    }

    private String podName() {
        String pod = System.getenv("POD_NAME");
        if (pod != null && !pod.isBlank()) {
            return pod;
        }
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
