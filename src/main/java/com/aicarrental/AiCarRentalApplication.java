package com.aicarrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class AiCarRentalApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiCarRentalApplication.class, args);
	}

}
