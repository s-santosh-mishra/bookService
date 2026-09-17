package com.servicehub.serv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServApplication.class, args);
	}

}
