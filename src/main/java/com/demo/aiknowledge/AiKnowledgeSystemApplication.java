package com.demo.aiknowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.demo.aiknowledge.mapper")
public class AiKnowledgeSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiKnowledgeSystemApplication.class, args);
    }

}
