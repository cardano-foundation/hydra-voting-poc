package org.cardanofoundation.hydrapoc.common;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.backend.api.*;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import org.cardanofoundation.hydrapoc.util.TransactionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class AppConfiguration {

    @Value("${cardano.network:#{null}}")
    private String network;

    @Value("${bf.api.url:#{null}}")
    private String bfUrl;

    @Value("${bf.project_id:#{null}}")
    private String bfProjectId;

    @Bean
    @ConditionalOnProperty(prefix = "bf", name = "api.url")
    @Primary
    public BackendService getBackendService() {
        return new BFBackendService(bfUrl, bfProjectId);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bf", name = "api.url")
    @Primary
    public UtxoSupplier getUtxoSupplier() {
        return new DefaultUtxoSupplier(getBackendService().getUtxoService());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bf", name = "api.url")
    @Primary
    public ProtocolParamsSupplier getProtocolParamsSupplier() {
        return new DefaultProtocolParamsSupplier(getBackendService().getEpochService());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bf", name = "api.url")
    @Primary
    public TransactionProcessor getTransactionProcessor() {
        return new DefaultTransactionProcessor(getBackendService().getTransactionService());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bf", name = "api.url")
    @Primary
    public TransactionService getTransactionService() {
        return getBackendService().getTransactionService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bf", name = "api.url")
    @Primary
    public TransactionUtil getTransactionUtil() {
        return new TransactionUtil(getBackendService().getTransactionService());
    }

    @Bean
    @ConditionalOnProperty(prefix = "hydra", name = "ws.url")
    @Primary
    public TransactionUtil getHydraTransactionUtil() {
        return new TransactionUtil(null);
    }

}
