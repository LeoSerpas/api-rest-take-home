package com.ols;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Habilita la ejecución de tareas programadas (@Scheduled)
public class ApiRestTakeHomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiRestTakeHomeApplication.class, args);
	}

}
