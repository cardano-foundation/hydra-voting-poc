package org.cardanofoundation.hydrapoc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HydraVoteApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(HydraVoteApplication.class)
                .logStartupInfo(false)
                .run(args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
