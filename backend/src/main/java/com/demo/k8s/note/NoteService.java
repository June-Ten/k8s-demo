package com.demo.k8s.note;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);
    static final String CACHE_KEY = "notes:all";

    private final NoteRepository repository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int ttlSeconds;

    public NoteService(
            NoteRepository repository,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.cache-ttl-seconds:30}") int ttlSeconds) {
        this.repository = repository;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
    }

    public NoteListResponse list() {
        String cached = readCache();
        if (cached != null) {
            List<NoteResponse> notes = decode(cached);
            if (notes != null) {
                return new NoteListResponse(notes, true, ttlSeconds);
            }
        }

        List<NoteResponse> notes = repository.findAllByOrderByIdDesc().stream()
                .map(NoteResponse::from)
                .toList();
        writeCache(notes);
        return new NoteListResponse(notes, false, ttlSeconds);
    }

    @Transactional
    public NoteResponse create(NoteRequest request) {
        String author = request.author() == null || request.author().isBlank()
                ? "匿名"
                : request.author().trim();
        Note saved = repository.save(new Note(author, request.content().trim()));
        evict();
        return NoteResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "留言不存在");
        }
        repository.deleteById(id);
        evict();
    }

    private String readCache() {
        try {
            return redis.opsForValue().get(CACHE_KEY);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void writeCache(List<NoteResponse> notes) {
        try {
            String json = objectMapper.writeValueAsString(notes);
            redis.opsForValue().set(CACHE_KEY, json, Duration.ofSeconds(ttlSeconds));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Redis 写入失败，改为直接返回数据库结果", ex);
        }
    }

    private void evict() {
        try {
            redis.delete(CACHE_KEY);
        } catch (RuntimeException ex) {
            // 缓存失效失败时，最多保留一个 TTL 的旧列表。
        }
    }

    private List<NoteResponse> decode(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            evict();
            return null;
        }
    }
}
