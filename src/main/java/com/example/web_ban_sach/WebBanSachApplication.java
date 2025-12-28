package com.example.web_ban_sach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebBanSachApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebBanSachApplication.class, args);
	}

}
