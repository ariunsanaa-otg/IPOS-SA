package com.infopharma.ipos_sa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IposSaApplication {

	public static void main(String[] args) {
		SpringApplication.run(IposSaApplication.class, args);
	}

}
