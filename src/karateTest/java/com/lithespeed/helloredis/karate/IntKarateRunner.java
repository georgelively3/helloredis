package com.lithespeed.helloredis.karate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntKarateRunner {

    @Test
    void runAll() {
        Results results = Runner.path("classpath:karate")
                .karateEnv("int")
                .parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
