package com.recaring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class RecaringApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecaringApplication.class, args);
	}

}
