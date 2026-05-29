package com.lithespeed.helloredis.service;

import com.lithespeed.helloredis.model.Dialog;
import com.lithespeed.helloredis.repository.DialogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DialogServiceTest {

    @Mock
    private DialogRepository dialogRepository;

    @InjectMocks
    private DialogService dialogService;

    @Test
    void getAllDialogs_returnsAllFromRepository() {
        when(dialogRepository.findAll()).thenReturn(List.of(
                new Dialog(1, "hello", "world"),
                new Dialog(2, "foo", "bar")));

        assertThat(dialogService.getAllDialogs()).hasSize(2);
    }

    @Test
    void getAllDialogs_emptyRepository_returnsEmptyList() {
        when(dialogRepository.findAll()).thenReturn(List.of());

        assertThat(dialogService.getAllDialogs()).isEmpty();
    }

    @Test
    void getDialogByIdAndRequest_matchingIdAndRequest_returnsDialog() {
        Dialog dialog = new Dialog(1, "hello", "world");
        when(dialogRepository.findById(1)).thenReturn(Optional.of(dialog));

        assertThat(dialogService.getDialogByIdAndRequest(1, "hello"))
                .isPresent().contains(dialog);
    }

    @Test
    void getDialogByIdAndRequest_requestMismatch_returnsEmpty() {
        when(dialogRepository.findById(1)).thenReturn(Optional.of(new Dialog(1, "hello", "world")));

        assertThat(dialogService.getDialogByIdAndRequest(1, "wrong")).isEmpty();
    }

    @Test
    void getDialogByIdAndRequest_idNotFound_returnsEmpty() {
        when(dialogRepository.findById(99)).thenReturn(Optional.empty());

        assertThat(dialogService.getDialogByIdAndRequest(99, "hello")).isEmpty();
    }

    @Test
    void getDialogByIdAndRequest_nullRequest_returnsEmpty() {
        assertThat(dialogService.getDialogByIdAndRequest(1, null)).isEmpty();
    }

    @Test
    void updateDialog_existingId_setsIdAndReturnsUpdated() {
        Dialog updated = new Dialog(0, "new request", "new response");
        when(dialogRepository.existsById(1)).thenReturn(true);
        when(dialogRepository.save(any(Dialog.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Dialog> result = dialogService.updateDialog(1, updated);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1);
        assertThat(result.get().getRequest()).isEqualTo("new request");
    }

    @Test
    void updateDialog_nonExistentId_returnsEmpty() {
        when(dialogRepository.existsById(99)).thenReturn(false);

        assertThat(dialogService.updateDialog(99, new Dialog())).isEmpty();
        verify(dialogRepository, never()).save(any());
    }

    @Test
    void deleteDialog_existingId_returnsTrue() {
        when(dialogRepository.existsById(1)).thenReturn(true);

        assertThat(dialogService.deleteDialog(1)).isTrue();
        verify(dialogRepository).deleteById(1);
    }

    @Test
    void deleteDialog_nonExistentId_returnsFalse() {
        when(dialogRepository.existsById(99)).thenReturn(false);

        assertThat(dialogService.deleteDialog(99)).isFalse();
        verify(dialogRepository, never()).deleteById(anyInt());
    }
}
