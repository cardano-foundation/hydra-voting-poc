package org.cardanofoundation.hydrapoc.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Choice {
    ABSTAIN, NAY, YAY;

    @JsonValue
    public int toValue() {
        return ordinal();
    }
}
