package com.metrica.porramundial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PorraMundialMetricaApplication {
    public static void main(String[] args) {
        SpringApplication.run(PorraMundialMetricaApplication.class, args);
    }
}