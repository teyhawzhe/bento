package com.lovius.bento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BentoApplication.class, args);
    }
}
