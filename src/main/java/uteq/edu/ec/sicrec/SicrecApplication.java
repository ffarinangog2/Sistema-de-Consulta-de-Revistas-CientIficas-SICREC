package uteq.edu.ec.sicrec;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class SicrecApplication {

    public static void main(String[] args) {
        SpringApplication.run(SicrecApplication.class, args);
    }

    @Bean
    CommandLineRunner generarHash() {
        return args -> {
            System.out.println(new BCryptPasswordEncoder().encode("admin123"));
        };
    }

}