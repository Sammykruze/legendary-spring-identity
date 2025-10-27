package com.legendaryUser.legendary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.legendaryUser.legendary")
@EnableScheduling
public class LegendaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegendaryApplication.class, args);
	}

}
