package com.jk.explore.security.cia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The store's order system with a control for confidentiality (what each role may see),
 * integrity (a seal checked on every read), and a logged emergency exception.
 */
public class SecureStore {

    /** A stored order together with the seal made when it was saved. */
    public record Stored(OrderRecord order, String seal) {
    }

    private final Seal sealer;
    private final Map<String, Stored> storage = new LinkedHashMap<>();
    private final List<String> auditLog = new ArrayList<>();

    public SecureStore(Seal sealer) {
        this.sealer = sealer;
    }

    /** Saves an order and seals it. */
    public void save(OrderRecord order) {
        storage.put(order.id(), new Stored(order, sealer.of(order)));
    }

    /** Confidentiality: finance sees everything; support sees customer and item only. */
    public OrderView read(User asker, String id) {
        OrderRecord order = verified(id);
        if (asker.role() == Role.FINANCE) {
            return new OrderView(id, order.customer(), order.item(), order.address(), order.amountDollars());
        }
        return new OrderView(id, order.customer(), order.item(), null, null);
    }

    /** Integrity: only finance may change an amount, and the order is sealed again. */
    public void setAmount(User asker, String id, int amountDollars) {
        if (asker.role() != Role.FINANCE) {
            throw new AccessDeniedException(asker.name() + " may not change an amount");
        }
        save(verified(id).withAmount(amountDollars));
    }

    /**
     * The cost of strict confidentiality is that nobody else can help in an emergency. This
     * exception lets any employee read the shipping address, but only with a reason, and it is logged.
     */
    public OrderView breakGlass(User asker, String id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AccessDeniedException("emergency access needs a reason");
        }
        OrderRecord order = verified(id);
        auditLog.add(asker.name() + " used emergency access on " + id + ": " + reason);
        return new OrderView(id, order.customer(), order.item(), order.address(), null);
    }

    public List<String> auditLog() {
        return List.copyOf(auditLog);
    }

    /**
     * Gives direct access to the stored data, standing in for someone who reaches the database
     * without going through the application. Used only to demonstrate tampering.
     */
    public Map<String, Stored> storageForDemo() {
        return storage;
    }

    private OrderRecord verified(String id) {
        Stored stored = storage.get(id);
        if (!sealer.matches(stored.order(), stored.seal())) {
            throw new TamperedOrderException(id + " does not match its seal");
        }
        return stored.order();
    }
}
