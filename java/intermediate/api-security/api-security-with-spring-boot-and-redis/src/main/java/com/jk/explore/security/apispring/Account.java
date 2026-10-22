package com.jk.explore.security.apispring;

/**
 * The bank's record of a customer, as it is stored. It has setters for every field, which is what lets a JSON body bind to it field by field:
 * convenient for the first version of the API, and the reason the second version never binds a body to this class.
 */
public class Account {

    private String id;
    private String displayName;
    private String email;
    private String role = "customer";
    private long balanceCents;
    private long creditLimitCents = 50_000;
    private int riskScore;
    private String cardNumber;
    private String pinHash;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(long balanceCents) {
        this.balanceCents = balanceCents;
    }

    public long getCreditLimitCents() {
        return creditLimitCents;
    }

    public void setCreditLimitCents(long creditLimitCents) {
        this.creditLimitCents = creditLimitCents;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }
}
