package edu.ustb.eldercarebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务功能
public class ElderCareBackendApplication {

    public static void main(String[] args) {

        
        SpringApplication.run(ElderCareBackendApplication.class, args);
        

    }

}
