package com.platform.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class CoreServiceApplication {

    static {
        // Java calls India's zone "Asia/Calcutta"; newer Postgres rejects that alias.
        // Set the modern name before Spring opens the DB connection.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}