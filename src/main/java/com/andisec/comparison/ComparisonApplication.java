package com.andisec.comparison;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ComparisonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComparisonApplication.class, args);
    }

}
