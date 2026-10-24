package com.platform.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        // Java calls India's zone "Asia/Calcutta"; newer Postgres rejects that alias.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(UserServiceApplication.class, args);
    }
}