package com.jk.explore.security.riskmgmt;

import java.util.List;
import java.util.Map;

/** The bank's register and its controls: made-up numbers in dollars, with ranges, the way an assessment workshop would produce them. */
public final class Register {

    private Register() {
    }

    public static final List<Risk> RISKS = List.of(
            new Risk("ransomware", "Ransomware on the core banking system", 2, 5, new Tri(0.05, 0.15, 0.5), new Tri(500_000, 3_000_000, 20_000_000)),
            new Risk("card-breach", "Cardholder data breach", 3, 4, new Tri(0.05, 0.2, 0.6), new Tri(300_000, 1_500_000, 8_000_000)),
            new Risk("insider-fraud", "Insider fraud in payments", 4, 3, new Tri(0.2, 0.6, 2), new Tri(20_000, 150_000, 2_000_000)),
            new Risk("third-party", "Breach at a third-party processor", 3, 3, new Tri(0.1, 0.4, 1.2), new Tri(50_000, 400_000, 5_000_000)),
            new Risk("outage", "Payments outage", 5, 2, new Tri(0.5, 2, 6), new Tri(10_000, 60_000, 400_000)),
            new Risk("phishing", "Phishing leading to account takeover", 5, 2, new Tri(2, 6, 15), new Tri(5_000, 25_000, 150_000)),
            new Risk("ddos", "Denial of service on the web site", 3, 2, new Tri(0.5, 2, 6), new Tri(5_000, 40_000, 300_000)),
            new Risk("laptop", "Lost or stolen laptop", 4, 1, new Tri(1, 3, 8), new Tri(2_000, 10_000, 80_000)));

    public static final List<Control> CONTROLS = List.of(
            new Control("backups", "Offline immutable backups", 250_000, Map.of("ransomware", new Control.Effect(1, 0.3))),
            new Control("edr", "Endpoint detection and network segmentation", 400_000, Map.of("ransomware", new Control.Effect(0.5, 1), "card-breach", new Control.Effect(0.8, 1))),
            new Control("mfa", "Multi-factor login and phishing training", 120_000, Map.of("phishing", new Control.Effect(0.4, 1), "ransomware", new Control.Effect(0.8, 1))),
            new Control("tokens", "Tokenization of card data", 500_000, Map.of("card-breach", new Control.Effect(1, 0.2))),
            new Control("vendors", "Vendor assessments and contract clauses", 150_000, Map.of("third-party", new Control.Effect(0.6, 0.8))),
            new Control("approval", "Dual approval of payments", 90_000, Map.of("insider-fraud", new Control.Effect(1, 0.3))),
            new Control("scrubbing", "Traffic scrubbing service", 200_000, Map.of("ddos", new Control.Effect(1, 0.1), "outage", new Control.Effect(0.8, 1))));
}
