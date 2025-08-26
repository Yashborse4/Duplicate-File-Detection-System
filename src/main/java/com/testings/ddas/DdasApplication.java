package com.testings.ddas;

import com.testings.ddas.Services.DirectoryWatcher;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.nio.file.Paths;

@SpringBootApplication
@EntityScan(basePackages = "com.testings.ddas.Model")
public class DdasApplication implements CommandLineRunner {

	@Autowired
	private DirectoryWatcher directoryWatcher;

	public static void main(String[] args) {
		SpringApplication.run(DdasApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		directoryWatcher.watchDirectory(Paths.get("D:/"));
	}
}