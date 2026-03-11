package org.example.atuo_attend_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.atuo_attend_backend.**.mapper")
public class AtuoAttendBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtuoAttendBackendApplication.class, args);
    }

}
