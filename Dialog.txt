package com.lithespeed.helloredis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true, allowSetters = false)
@RedisHash("dialogs")
public class Dialog implements Serializable {

    public interface RequestView {}
    public interface ResponseView {}
    public interface RequestResponseView {}

    @JsonView({RequestView.class, ResponseView.class, RequestResponseView.class})
    @Id
    private int id;

    @JsonView({RequestView.class, RequestResponseView.class})
    private String request;

    @JsonView({ResponseView.class, RequestResponseView.class})
    private String response;
}
