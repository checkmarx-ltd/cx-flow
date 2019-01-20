package com.custodela.machina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MachinaApplication {
	public static void main(String[] args) {
		SpringApplication.run(MachinaApplication.class, args);
	}
}
