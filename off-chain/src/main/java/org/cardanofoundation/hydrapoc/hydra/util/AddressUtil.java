package org.cardanofoundation.hydrapoc.hydra.util;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;

public class AddressUtil {

    public static String getAddressFromVerificationKey(String vkCborHex) throws Exception {
        VerificationKey vk = new VerificationKey(vkCborHex);
        HdPublicKey hdPublicKey = new HdPublicKey();
        hdPublicKey.setKeyData(vk.getBytes());
        Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());

        return address.toBech32();
    }

    public static void main(String[] args) throws Exception {
        String address1 = getAddressFromVerificationKey("58209538cb2039a151051a158a58c23c1fcf74f2ec5e94a7c5da186d3fa1e60e825d");
        //addr_test1vr2jvlvw62kv82x8gn0pewn6n5r82m6zxxn6c7vp04t9avs3wgpxv
        System.out.println(address1);

        String address2 = getAddressFromVerificationKey("5820690290082ab33e6ad5dc2f1e37a4e82fc1017438460da9a00189e2d1929c43c1");
        //│addr_test1vz4jpljkq88278xat56pcy240ey9ng9wza8qtdavg6f7vqs0z8903
        System.out.println(address2);

        String address3 = getAddressFromVerificationKey("58202e6a3289f00c4cb1f1bac47aa1532ce153b0084502b44f8c02eaa482eec5bf3f");
        //addr_test1vzh03tyuujtl4tfq4maduaxk0pvt893xy4g4l6cn4k7mtxs7rmsjz
        System.out.println(address3);

    }
}
