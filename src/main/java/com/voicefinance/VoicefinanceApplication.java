package com.voicefinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@RestController
public class VoicefinanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoicefinanceApplication.class, args);
	}

}
