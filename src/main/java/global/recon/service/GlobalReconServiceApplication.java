package global.recon.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "global.recon.service.feignclient")
public class GlobalReconServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlobalReconServiceApplication.class, args);
    }
}
