package com.agrishield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgriShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriShieldApplication.class, args);
    }
}
