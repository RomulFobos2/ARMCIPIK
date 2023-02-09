package com.mai.ARMCIPIK;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class ArmcipikApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArmcipikApplication.class, args);
	}

}
