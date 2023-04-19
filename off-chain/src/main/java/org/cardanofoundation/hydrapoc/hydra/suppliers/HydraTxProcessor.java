package org.cardanofoundation.hydrapoc.hydra.suppliers;

import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.model.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.cardanofoundation.hydrapoc.hydra.TxResult;
import org.cardanofoundation.hydrapoc.util.ByteUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
@Slf4j
public class HydraTxProcessor implements TransactionProcessor {

    private final HydraClient hydraClient;

    @Override
    public Result<String> submitTransaction(byte[] txCbor) {
        log.info("Transaction size: {}", ByteUtil.humanReadableByteCountBin(txCbor.length));

        Mono<TxResult> mono = hydraClient.submitTxFullConfirmation(txCbor);

        TxResult txResult = mono.block(Duration.ofMinutes(10));

        return Result.create(txResult.isValid(), txResult.getMessage())
                .withValue(txResult.getTxId());
    }

}
