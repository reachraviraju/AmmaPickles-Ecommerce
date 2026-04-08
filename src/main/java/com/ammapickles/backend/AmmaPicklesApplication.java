package com.ammapickles.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableCaching
@EnableMethodSecurity
@SpringBootApplication
public class AmmaPicklesApplication {

	public static void main(String[] args) {
		
		
		SpringApplication.run(AmmaPicklesApplication.class, args);
		
		
	
		
	}

} 
