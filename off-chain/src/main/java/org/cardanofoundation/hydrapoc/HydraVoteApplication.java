package org.cardanofoundation.hydrapoc;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class HydraVoteApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(HydraVoteApplication.class)
                .logStartupInfo(false)
                .run(args);
    }

}
