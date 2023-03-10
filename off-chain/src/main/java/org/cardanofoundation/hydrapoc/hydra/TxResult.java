package org.cardanofoundation.hydrapoc.hydra;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TxResult {
    private String txId;
    private boolean isValid;
    private String message;
}
