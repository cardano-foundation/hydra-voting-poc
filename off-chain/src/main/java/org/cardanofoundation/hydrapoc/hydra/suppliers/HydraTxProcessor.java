package org.cardanofoundation.hydrapoc.hydra.suppliers;

import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.model.Result;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.cardanofoundation.hydrapoc.hydra.TxResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
public class HydraTxProcessor implements TransactionProcessor {
    private final HydraClient hydraClient;

    @Override
    public Result<String> submitTransaction(byte[] txCbor) {
        Mono<TxResult> mono = hydraClient.submitTx(txCbor);
        TxResult txResult = mono.block(Duration.ofSeconds(100)); //TODO -- make it non blocking

        return Result.create(txResult.isValid(), txResult.getMessage())
                .withValue(txResult.getTxId());

    }
}
