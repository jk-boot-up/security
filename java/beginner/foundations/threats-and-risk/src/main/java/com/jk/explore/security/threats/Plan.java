package com.jk.explore.security.threats;

import java.util.List;

/** The treatments that were paid for, what they cost, and the register as it looks afterwards. */
public record Plan(List<Treatment> funded, List<Treatment> unfunded, int spent, int budget, RiskRegister after) {
}
