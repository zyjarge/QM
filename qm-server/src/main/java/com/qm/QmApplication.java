package com.qm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("com.qm.**.mapper")
public class QmApplication {

    public static void main(String[] args) {
        SpringApplication.run(QmApplication.class, args);
        System.out.println("""
            ========================================
              QM Server started successfully!
              API Docs: http://localhost:8080/swagger-ui.html
            ========================================
            """);
    }
}
