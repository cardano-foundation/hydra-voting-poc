package org.cardanofoundation.hydrapoc.hydra;

import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraStateEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Transaction;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.response.*;
import org.cardanofoundation.hydrapoc.store.InMemoryUTxOStore;
import org.cardanofoundation.hydrapoc.store.UTxOStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
public class HydraClient implements HydraStateEventListener, HydraQueryEventListener {

    private HydraWSClient hydraWSClient;

    @Value("${hydra.ws.url}")
    private String hydraWsUrl;

    private Map<String, List<MonoSink>> monoSinkMap = new ConcurrentHashMap<>();

    private final UTxOStore uTxOStore = new InMemoryUTxOStore();

    public HydraClient() {
    }

    public Mono<Void> connect() {
        return connectInternally(-1);
    }

    public Mono<Void> connect(int fromSeq) {
        return connectInternally(fromSeq);
    }
    public void disconnect() {
        if (hydraWSClient != null) {
            hydraWSClient.close();
        }
    }

    public UTxOStore getUTxOStore() {
        return uTxOStore;
    }

    private Mono<Void> connectInternally(final int fromSeq) {
        if (hydraWSClient != null && hydraWSClient.isOpen()) {
            return Mono.empty();
        }

        if (fromSeq > 0) {
            var options = HydraClientOptions.builder(hydraWsUrl)
                    .fromSeq(fromSeq)
                    .build();

            this.hydraWSClient = new HydraWSClient(options);
        } else {
            this.hydraWSClient = new HydraWSClient(HydraClientOptions.createDefault(hydraWsUrl));
        }
        hydraWSClient.addHydraQueryEventListener(this);
        hydraWSClient.addHydraStateEventListener(this);

        return Mono.create(monoSink -> {
            storeMonoSinkReference(new HydraConnectRequest().key(), monoSink);
            hydraWSClient.connect();
        });
    }

    @Override
    public void onResponse(Response response) {
        log.info("Tag:{}, seq:{}", response.getTag(), response.getSeq());

        if (response instanceof HeadIsOpenResponse ho) {
            // we get initial UTxOs here as well
            var utxo = ho.getUtxo();
            uTxOStore.storeLatestUtxO(utxo);
        }

        if (response instanceof SnapshotConfirmed sc) {
            Map<String, UTXO> utxo = sc.getSnapshot().getUtxo();
            uTxOStore.storeLatestUtxO(utxo);

            for (Transaction trx : sc.getSnapshot().getConfirmedTransactions()) {
                TxResult txResult = new TxResult(trx.getId(), trx.getIsValid());

                TxGlobalRequest txGlobalRequest = TxGlobalRequest.of(trx.getId());
                applyMonoSuccess(txGlobalRequest.key(), txResult);
            }
        }

        if (response instanceof GreetingsResponse) {
            applyMonoSuccess(new HydraConnectRequest().key());
        }

        if (response instanceof TxValidResponse txResponse) {
            String txId = txResponse.getTransaction().getId();
            TxResult txResult = new TxResult(txId, true);

            applyMonoSuccess(TxLocalRequest.of(txId).toString(), txResult);
        }
        if (response instanceof TxInvalidResponse txResponse) {
            String txId = txResponse.getTransaction().getId();
            String reason = txResponse.getValidationError().getReason();
            TxResult txResult = new TxResult(txId, true, reason);

            applyMonoSuccess(TxLocalRequest.of(txId).key(), txResult);
        }
    }

    @Override
    public void onStateChanged(HydraState prevState, HydraState newState) {
        log.info("On StateChange Prev State: {}, new state:{}", prevState, newState);
    }

    public HydraState getHydraState() {
        return hydraWSClient.getHydraState();
    }

    public Mono<TxResult> submitTx(byte[] cborTx) {
        return Mono.create(monoSink -> {
            String txHash = TransactionUtil.getTxHash(cborTx);
            storeMonoSinkReference(TxLocalRequest.of(txHash).key(), monoSink);
            hydraWSClient.newTx(HexUtil.encodeHexString(cborTx));
        });
    }

    public Mono<TxResult> submitTxFullConfirmation(byte[] cborTx) {

        return Mono.create(monoSink -> {
            String txHash = TransactionUtil.getTxHash(cborTx);
            log.info("Submitting tx:" + txHash);

            storeMonoSinkReference(TxGlobalRequest.of(txHash).key(), monoSink);
            hydraWSClient.newTx(HexUtil.encodeHexString(cborTx));
        });
    }

    protected <T extends Request> void storeMonoSinkReference(String key, MonoSink monoSink) {
        monoSinkMap.computeIfAbsent(key, k -> {
            var list = new ArrayList<MonoSink>();
            list.add(monoSink);

            return list;
        });
    }

    protected <T extends Request> void applyMonoSuccess(String key, Object result) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null) {
            return;
        }

        monoSinks.forEach(monoSink -> monoSink.success(result));
    }

    protected <T extends Request> void applyMonoSuccess(String key) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null) {
            return;
        }

        monoSinks.forEach(MonoSink::success);
    }

    protected <T extends Request> void applyMonoError(String key, Object result) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null) {
            return;
        }

        monoSinks.forEach(monoSink -> monoSink.error(new RuntimeException(String.valueOf(result))));
    }

}
