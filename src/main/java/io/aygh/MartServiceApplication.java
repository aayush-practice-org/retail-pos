package io.aygh;

import io.aygh.config.properties.TaxProperties;
import io.aygh.config.properties.TokenProperties;
import me.paulschwarz.springdotenv.spring.DotenvApplicationInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;


@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableConfigurationProperties({TokenProperties.class, TaxProperties.class})
public class MartServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MartServiceApplication.class);
        application.addInitializers(new DotenvApplicationInitializer());
        application.run(args);
    }
}
