package com.jk.explore.security.accesscontrol;

/** An order in the store. The address can be changed, so it is not a record. */
final class Order {

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

    String id() {
        return id;
    }

    String owner() {
        return owner;
    }

    String item() {
        return item;
    }

    String address() {
        return address;
    }

    void moveTo(String newAddress) {
        address = newAddress;
    }
}
