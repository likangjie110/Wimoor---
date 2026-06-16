package com.wimoor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.wimoor.util.SpringUtil;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wimoor")
@EnableCaching
@EnableTransactionManagement
@MapperScan({
        "com.wimoor.ozon.**.mapper",
        "com.wimoor.common.mapper"
})
public class OzonApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(OzonApplication.class, args);
        SpringUtil.set(context);
    }
}
