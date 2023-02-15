package org.cardanofoundation.hydrapoc.voteimporter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class HydraVoteImporterApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(HydraVoteImporterApplication.class)
                .logStartupInfo(false)
                .run(args);
    }

}
