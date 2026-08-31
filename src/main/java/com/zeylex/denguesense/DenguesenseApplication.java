package com.zeylex.denguesense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DenguesenseApplication {

	static {
		// Docker/JRE hosts default to UTC. Timestamps are LocalDateTime (no offset),
		// so the JVM must use Sri Lanka time or live data shows ~5.5h behind.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"));
	}

	public static void main(String[] args) {
		SpringApplication.run(DenguesenseApplication.class, args);
	}

}
