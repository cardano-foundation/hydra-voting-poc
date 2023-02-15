package org.cardanofoundation.hydrapoc.voteimporter.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public record Vote(
        String voterKey,
        long votingPower,
        long challenge,
        long proposal,
        Choice choice
) {
}
