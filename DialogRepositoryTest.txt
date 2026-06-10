package com.lithespeed.helloredis.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.helloredis.model.Dialog;
import com.lithespeed.helloredis.model.DialogResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DialogRepositoryTest {

    private static final String HASH_KEY = "dialogs";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private DialogRepository dialogRepository;

    @BeforeEach
    void setUp() {
        // lenient: some tests return early before calling opsForHash()
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        dialogRepository = new DialogRepository(redisTemplate, objectMapper);
    }

    // --- findAll ---

    @Test
    void findAll_withEntries_returnsMappedDialogs() {
        Object raw1 = new Object();
        Object raw2 = new Object();
        Dialog dialog1 = new Dialog(1, "hello", "world");
        Dialog dialog2 = new Dialog(2, "foo", "bar");
        when(hashOperations.values(HASH_KEY)).thenReturn(List.of(raw1, raw2));
        when(objectMapper.convertValue(raw1, Dialog.class)).thenReturn(dialog1);
        when(objectMapper.convertValue(raw2, Dialog.class)).thenReturn(dialog2);

        assertThat(dialogRepository.findAll()).containsExactly(dialog1, dialog2);
    }

    @Test
    void findAll_emptyHash_returnsEmptyList() {
        when(hashOperations.values(HASH_KEY)).thenReturn(List.of());

        assertThat(dialogRepository.findAll()).isEmpty();
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsDialog() {
        Object raw = new Object();
        Dialog dialog = new Dialog(1, "hello", "world");
        when(hashOperations.get(HASH_KEY, "1")).thenReturn(raw);
        when(objectMapper.convertValue(raw, Dialog.class)).thenReturn(dialog);

        assertThat(dialogRepository.findById(1)).isPresent().contains(dialog);
    }

    @Test
    void findById_missingId_returnsEmpty() {
        when(hashOperations.get(HASH_KEY, "99")).thenReturn(null);

        assertThat(dialogRepository.findById(99)).isEmpty();
    }

    // --- getDialogByIdAndRequest ---

    @Test
    void getDialogByIdAndRequest_matchingIdAndRequest_returnsDTO() {
        Object raw = new Object();
        Dialog dialog = new Dialog(1, "hello", "world");
        when(hashOperations.get(HASH_KEY, "1")).thenReturn(raw);
        when(objectMapper.convertValue(raw, Dialog.class)).thenReturn(dialog);

        Optional<DialogResponseDTO> result = dialogRepository.getDialogByIdAndRequest(1, "hello");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1);
        assertThat(result.get().getResponse()).isEqualTo("world");
    }

    @Test
    void getDialogByIdAndRequest_requestMismatch_returnsEmpty() {
        Object raw = new Object();
        Dialog dialog = new Dialog(1, "hello", "world");
        when(hashOperations.get(HASH_KEY, "1")).thenReturn(raw);
        when(objectMapper.convertValue(raw, Dialog.class)).thenReturn(dialog);

        assertThat(dialogRepository.getDialogByIdAndRequest(1, "wrong")).isEmpty();
    }

    @Test
    void getDialogByIdAndRequest_idNotFound_returnsEmpty() {
        when(hashOperations.get(HASH_KEY, "99")).thenReturn(null);

        assertThat(dialogRepository.getDialogByIdAndRequest(99, "hello")).isEmpty();
    }

    @Test
    void getDialogByIdAndRequest_nullRequest_returnsEmpty() {
        assertThat(dialogRepository.getDialogByIdAndRequest(1, null)).isEmpty();
        verifyNoInteractions(hashOperations);
    }

    // --- save ---

    @Test
    void save_putsDialogInHashAndReturnsIt() {
        Dialog dialog = new Dialog(1, "hello", "world");

        Dialog result = dialogRepository.save(dialog);

        verify(hashOperations).put(HASH_KEY, "1", dialog);
        assertThat(result).isEqualTo(dialog);
    }

    // --- deleteById ---

    @Test
    void deleteById_deletesFromHash() {
        dialogRepository.deleteById(1);

        verify(hashOperations).delete(HASH_KEY, "1");
    }

    // --- existsById ---

    @Test
    void existsById_existingKey_returnsTrue() {
        when(hashOperations.hasKey(HASH_KEY, "1")).thenReturn(true);

        assertThat(dialogRepository.existsById(1)).isTrue();
    }

    @Test
    void existsById_missingKey_returnsFalse() {
        when(hashOperations.hasKey(HASH_KEY, "1")).thenReturn(false);

        assertThat(dialogRepository.existsById(1)).isFalse();
    }

    @Test
    void existsById_nullFromRedis_returnsFalse() {
        when(hashOperations.hasKey(HASH_KEY, "1")).thenReturn(null);

        assertThat(dialogRepository.existsById(1)).isFalse();
    }
}
