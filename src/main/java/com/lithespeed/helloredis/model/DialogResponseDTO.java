package com.lithespeed.helloredis.model;

public class DialogResponseDTO {

    private final int id;
    private final String response;

    public DialogResponseDTO(int id, String response) {
        this.id = id;
        this.response = response;
    }

    public int getId() {
        return id;
    }

    public String getResponse() {
        return response;
    }
}