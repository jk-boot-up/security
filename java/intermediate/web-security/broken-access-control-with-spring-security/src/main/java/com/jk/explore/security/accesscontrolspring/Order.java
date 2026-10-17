package com.jk.explore.security.accesscontrolspring;

/** An order in the store. The address can be changed, so it is not a record. */
public final class Order {

    private final String id;
    private final String owner;
    private final String item;
    private String address;

    Order(String id, String owner, String item, String address) {
        this.id = id;
        this.owner = owner;
        this.item = item;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    /** Named for SpEL: {@code returnObject.owner} in a {@code @PostAuthorize} rule reads this. */
    public String getOwner() {
        return owner;
    }

    public String getItem() {
        return item;
    }

    public String getAddress() {
        return address;
    }

    void moveTo(String newAddress) {
        address = newAddress;
    }

    String describe() {
        return "order " + id + ": " + item + " for " + owner + ", to " + address;
    }
}
