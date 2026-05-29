package com.lithespeed.helloredis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.helloredis.model.Dialog;
import com.lithespeed.helloredis.service.DialogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DialogController.class)
class DialogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DialogService dialogService;

    @Test
    void getAllDialogs_returnsOkWithList() throws Exception {
        when(dialogService.getAllDialogs()).thenReturn(List.of(
                new Dialog(1, "hello", "world")));

        mockMvc.perform(get("/api/dialogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].request").value("hello"));
    }

    @Test
    void getAllDialogs_emptyList_returnsOk() throws Exception {
        when(dialogService.getAllDialogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/dialogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getDialog_found_returnsOk() throws Exception {
        Dialog dialog = new Dialog(1, "hello", "world");
        when(dialogService.getDialogByIdAndRequest(1, "hello")).thenReturn(Optional.of(dialog));

        mockMvc.perform(get("/api/dialogs/1").param("request", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("world"));
    }

    @Test
    void getDialog_notFound_returns404() throws Exception {
        when(dialogService.getDialogByIdAndRequest(99, "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dialogs/99").param("request", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDialog_returnsCreated() throws Exception {
        Dialog dialog = new Dialog(1, "hello", "world");
        when(dialogService.createDialog(any(Dialog.class))).thenReturn(dialog);

        mockMvc.perform(post("/api/dialogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dialog)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateDialog_found_returnsOk() throws Exception {
        Dialog updated = new Dialog(1, "updated request", "updated response");
        when(dialogService.updateDialog(eq(1), any(Dialog.class))).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/dialogs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request").value("updated request"));
    }

    @Test
    void updateDialog_notFound_returns404() throws Exception {
        when(dialogService.updateDialog(eq(99), any(Dialog.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/dialogs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Dialog())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDialog_found_returnsNoContent() throws Exception {
        when(dialogService.deleteDialog(1)).thenReturn(true);

        mockMvc.perform(delete("/api/dialogs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDialog_notFound_returns404() throws Exception {
        when(dialogService.deleteDialog(99)).thenReturn(false);

        mockMvc.perform(delete("/api/dialogs/99"))
                .andExpect(status().isNotFound());
    }
}
