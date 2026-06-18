package com.lithespeed.helloredis.logger;

public class MyLogger {

    public static void debugLog(String message) {
        try {
            java.nio.file.Path path = java.nio.file.Path.of("/tmp/helloredis-debug.txt");
            String line = java.time.Instant.now() + " " + message + System.lineSeparator();
            java.nio.file.Files.writeString(path, line,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // best-effort; don't break startup if /tmp is unwritable
        }
    }

}
