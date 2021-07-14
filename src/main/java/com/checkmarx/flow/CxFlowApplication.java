package com.checkmarx.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = { "com.checkmarx.sdk", "com.checkmarx.flow", "com.cx.restclient"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAsync
@EnableCaching
public class CxFlowApplication {
	public static void main(String[] args) {
		boolean web = Arrays.asList(args).contains("--web");

		if (args.length > 0 && !web) {
			SpringApplication app = new SpringApplication(CxFlowApplication.class);
			app.setWebApplicationType(WebApplicationType.NONE);
			app.run(args);
		} else {
			SpringApplication.run(CxFlowApplication.class, args);
		}
	}
}
