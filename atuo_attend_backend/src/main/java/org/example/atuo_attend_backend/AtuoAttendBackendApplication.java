package org.example.atuo_attend_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"org.example.atuo_attend_backend.**.mapper", "org.example.atuo_attend_backend.config"})
public class AtuoAttendBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtuoAttendBackendApplication.class, args);
    }

}
