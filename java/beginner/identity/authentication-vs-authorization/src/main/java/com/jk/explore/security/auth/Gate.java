package com.jk.explore.security.auth;

/** The check in front of every request: decides whether this token may do this action to this order. */
public interface Gate {
    Response check(String token, Action action, Order order);
}
