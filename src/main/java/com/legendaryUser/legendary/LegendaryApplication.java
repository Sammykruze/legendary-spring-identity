package com.legendaryUser.legendary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LegendaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegendaryApplication.class, args);
	}

}
