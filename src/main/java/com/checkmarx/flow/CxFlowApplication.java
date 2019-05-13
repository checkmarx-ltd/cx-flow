package com.checkmarx.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CxFlowApplication {
	public static void main(String[] args) {
		if(args != null) {
			SpringApplication.run(CxFlowApplication.class, args);
		}
	}
}
