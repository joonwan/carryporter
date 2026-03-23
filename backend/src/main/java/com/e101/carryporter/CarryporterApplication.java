package com.e101.carryporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling  // SSE 하트비트를 위한 스케줄링 활성화
@SpringBootApplication
public class CarryporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarryporterApplication.class, args);
    }


}
