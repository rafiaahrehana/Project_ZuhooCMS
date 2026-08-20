package com.businessos.modules.crm.opportunity;

public enum OpportunityStage {
    QUALIFICATION(15),
    PRESENTATION(35),
    PROPOSAL(55),
    NEGOTIATION(80),
    WON(100),
    LOST(0);

    private final int defaultProbability;

    OpportunityStage(int defaultProbability) {
        this.defaultProbability = defaultProbability;
    }

    public int getDefaultProbability() {
        return defaultProbability;
    }

    public boolean isClosed() {
        return this == WON || this == LOST;
    }
}
