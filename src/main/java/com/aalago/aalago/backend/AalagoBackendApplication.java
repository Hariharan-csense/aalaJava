package com.aalago.aalago.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AalagoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AalagoBackendApplication.class, args);
		System.out.println("Aalago backend started successfully!");
	}

}
