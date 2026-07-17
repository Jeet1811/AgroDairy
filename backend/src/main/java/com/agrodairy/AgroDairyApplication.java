package com.agrodairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgroDairyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgroDairyApplication.class, args);
	}

}
