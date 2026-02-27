package com.vibecode.ai_generation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AiGenerationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiGenerationServiceApplication.class, args);
	}

}
