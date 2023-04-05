package org.cardanofoundation.hydrapoc.hydra;

import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraStateEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.query.request.GetUTxORequest;
import org.cardanofoundation.hydra.client.model.query.response.*;
import org.cardanofoundation.hydrapoc.util.ByteUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
public class HydraClient implements HydraStateEventListener, HydraQueryEventListener {

    private HydraWSClient hydraWSClient;

    @Value("${hydra.ws.url}")
    private String hydraWsUrl;

    private Map<Object, List<MonoSink>> monoSinkMap = new ConcurrentHashMap<>();

    public HydraClient() {
    }

    public HydraClient connect() throws URISyntaxException, InterruptedException {
        connect(-1);
        return this;
    }

    private HydraClient connect(int fromSeq) throws URISyntaxException, InterruptedException {
        if (hydraWSClient != null && hydraWSClient.isOpen()) {
            return this;
        }

        if (fromSeq > 0) {
            this.hydraWSClient = new HydraWSClient(new URI(hydraWsUrl), fromSeq);
        } else {
            this.hydraWSClient = new HydraWSClient(new URI(hydraWsUrl));
        }
        hydraWSClient.setHydraQueryEventListener(this);
        hydraWSClient.setHydraStateEventListener(this);
        hydraWSClient.connectBlocking(60, TimeUnit.SECONDS);

        return this;
    }

    @Override
    public void onResponse(Response response) {
        log.info("Tag:{}, seq:{}", response.getTag(), response.getSeq());

        if (response instanceof SnapshotConfirmed sc) {
            log.info("Snapshot size:{}", sc.getSnapshot().getUtxo().size());
        }

        if (response instanceof GetUTxOResponse) {
            applyMonoSuccess(GetUTxORequest.class, response);
        } else if (response instanceof TxValidResponse txResponse) {
            String txId = txResponse.getTransaction().getId();
            applyMonoSuccess(new TxRequest(txId), new TxResult(txId, true, null));
        } else if (response instanceof TxInvalidResponse txResponse) {
            String txId = txResponse.getTransaction().getId();
            applyMonoSuccess(new TxRequest(txId), new TxResult(txId, false, txResponse.getValidationError().getReason()));
        }
    }

    @Override
    public void onStateChanged(HydraState prevState, HydraState newState) {
        log.info("On StateChange Prev State : {}", prevState);
        log.info("On StateChange New State : {}", newState);
    }

    public HydraState getHydraState() {
        return hydraWSClient.getHydraState();
    }

    // our connect method could be clever
    // and trigger connection event only
    // when we received the last message
    public HydraClient connectFrom(Optional<Integer> seq) throws URISyntaxException, InterruptedException {
        return connect(seq.orElse(-1));
    }

    public Mono<GetUTxOResponse> getUTXOs() {
        return Mono.create(monoSink -> {
            storeMonoSinkReference(GetUTxORequest.class, monoSink);
            hydraWSClient.getUTXO();
        });
    }

    public Mono<TxResult> submitTx(byte[] cborTx) {
        log.info("Transaction size: {}", ByteUtil.humanReadableByteCountBin(cborTx.length));

        return Mono.create(monoSink -> {
            storeMonoSinkReference(new TxRequest(TransactionUtil.getTxHash(cborTx)), monoSink);
            hydraWSClient.newTx(HexUtil.encodeHexString(cborTx));
        });
    }

    protected <T extends Request> void storeMonoSinkReference(Object key, MonoSink monoSink) {
        List<MonoSink> monoSinks = monoSinkMap.get(key);
        if (monoSinks == null) {
            monoSinks = new ArrayList<>();
            monoSinkMap.put(key, monoSinks);
        }
        monoSinks.add(monoSink);
    }

    protected <T extends Request> void applyMonoSuccess(Object key, Object result) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null)
            return;
        monoSinks.forEach(monoSink -> monoSink.success(result));
    }

    protected <T extends Request> void applyMonoError(Object key, Object result) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null)
            return;
        monoSinks.forEach(monoSink -> monoSink.error(new RuntimeException(String.valueOf(result))));
    }

}
