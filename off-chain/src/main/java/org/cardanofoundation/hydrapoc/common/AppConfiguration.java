package org.cardanofoundation.hydrapoc.common;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.backend.api.*;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class AppConfiguration {

    @Value("${cardano.network}")
    private String network;

    @Value("${bf.api.url}")
    private String bfUrl;

    @Value("${bf.project_id}")
    private String bfProjectId;

    @Bean
    public BackendService getBackendService() {
        return new BFBackendService(bfUrl, bfProjectId);
    }

    @Bean
    public UtxoSupplier getUtxoSupplier() {
        return new DefaultUtxoSupplier(getBackendService().getUtxoService());
    }

    @Bean
    public ProtocolParamsSupplier getProtocolParamsSupplier() {
        return new DefaultProtocolParamsSupplier(getBackendService().getEpochService());
    }

    @Bean
    public TransactionProcessor getTransactionProcessor() {
        return new DefaultTransactionProcessor(getBackendService().getTransactionService());
    }

    @Bean
    public TransactionService getTransactionService() {
        return getBackendService().getTransactionService();
    }
}
