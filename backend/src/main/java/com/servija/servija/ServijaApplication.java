package com.servija.servija;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServijaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServijaApplication.class, args);
	}

}
