package com.iquantex.phoenix.risk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author baozi
 * @date 2020/2/4 10:50 AM
 */
@Slf4j
@EnableSwagger2
@SpringBootApplication
public class PhoenixriskApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(PhoenixriskApplication.class, args);
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}

}
