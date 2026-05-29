package com.lithespeed.helloredis.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.helloredis.model.Dialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DialogRepository {

    private static final String HASH_KEY = "dialogs";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private HashOperations<String, String, Object> hashOps() {
        return redisTemplate.opsForHash();
    }

    public List<Dialog> findAll() {
        return hashOps().values(HASH_KEY).stream()
                .map(val -> objectMapper.convertValue(val, Dialog.class))
                .toList();
    }

    public Optional<Dialog> findById(int id) {
        Object val = hashOps().get(HASH_KEY, String.valueOf(id));
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(val, Dialog.class));
    }

    public Dialog save(Dialog dialog) {
        hashOps().put(HASH_KEY, String.valueOf(dialog.getId()), dialog);
        log.info("Saved dialog id={}", dialog.getId());
        return dialog;
    }

    public void deleteById(int id) {
        hashOps().delete(HASH_KEY, String.valueOf(id));
        log.info("Deleted dialog id={}", id);
    }

    public boolean existsById(int id) {
        return Boolean.TRUE.equals(hashOps().hasKey(HASH_KEY, String.valueOf(id)));
    }
}
