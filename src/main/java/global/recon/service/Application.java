package global.recon.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "global.recon.service.feignclient")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
