package com.system.batch.origin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OriginApplication {

    public static void main(String[] args) {

//        SpringApplication.run(OriginApplication.class, args);
        System.exit(SpringApplication.exit(SpringApplication.run(OriginApplication.class, args)));
    }

}
