package com.jk.explore.security.cia;

/** What one user is allowed to see of an order; a field they may not see is null. */
public record OrderView(String id, String customer, String item, String address, Integer amountDollars) {

    /** One printable line, with a hidden field shown as [hidden]. */
    public String describe() {
        return "customer=" + customer + ", item=" + item
                + ", address=" + (address == null ? "[hidden]" : address)
                + ", amount=" + (amountDollars == null ? "[hidden]" : "$" + amountDollars);
    }
}
