package com.wgc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@ServletComponentScan
@MapperScan("com.wgc.*.dao")
@SpringBootApplication
public class WGCApplication {
    public static void main(String[] args) {
        SpringApplication.run(WGCApplication.class, args);
        System.out.println("wgc启动成功");
    }
}
