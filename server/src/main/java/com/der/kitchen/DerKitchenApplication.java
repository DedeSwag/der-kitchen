package com.der.kitchen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@SpringBootApplication
public class DerKitchenApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(DerKitchenApplication.class, args);
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        String mvcPath = env.getProperty("spring.mvc.servlet.path");
        if (contextPath == null || contextPath.isEmpty()) {
            contextPath = "";
        }
        if (mvcPath == null || mvcPath.isEmpty()) {
            mvcPath = "";
        }
        log.info("\n----------------------------------------------------------\n\t" +
                "Application is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + contextPath + mvcPath + "/\n\t" +
                "External: \thttp://" + ip + ":" + port + contextPath + mvcPath + "/\n\t" +
                "Swagger: \thttp://" + ip + ":" + port + contextPath + mvcPath + "/swagger-ui.html\n\t" +
                "Swagger增强: \thttp://" + ip + ":" + port + contextPath + mvcPath + "/doc.html\n\t" +
                "----------------------------------------------------------");
    }
}
