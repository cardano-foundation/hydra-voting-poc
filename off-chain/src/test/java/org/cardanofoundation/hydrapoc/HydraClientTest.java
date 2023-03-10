package org.cardanofoundation.hydrapoc;

import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Utxo;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.cardanofoundation.hydrapoc.hydra.suppliers.HydraUtxoSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class HydraClientTest {
    @Autowired
    private HydraUtxoSupplier utxoSupplier;

    @Autowired
    private HydraClient hydraClient;

    @BeforeEach
    public void setup() {
    }

    @Test
    @Disabled
    public void getUtxos() {
        List<Utxo> utxoList = utxoSupplier.getPage("addr_test1vr2jvlvw62kv82x8gn0pewn6n5r82m6zxxn6c7vp04t9avs3wgpxv", 100, 0, OrderEnum.asc);
        System.out.println(utxoList);
        assertThat(utxoList).hasSizeGreaterThan(0);
    }
}
