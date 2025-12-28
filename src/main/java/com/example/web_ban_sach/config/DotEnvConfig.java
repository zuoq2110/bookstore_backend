package com.example.web_ban_sach.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DotEnvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> map = new HashMap<>();
            dotenv.entries().forEach(e -> map.put(e.getKey(), e.getValue()));

            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenv", map));

            System.out.println("✓ Loaded .env with " + map.size() + " variables");
        } catch (Exception e) {
            System.err.println("⚠ Warning: Could not load .env file: " + e.getMessage());
        }
    }
}
