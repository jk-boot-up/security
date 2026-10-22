package com.jk.explore.security.racepg;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** An account as Hibernate sees it: the version column is what makes the update conditional. */
@Entity
@Table(name = "hib_account")
public class HibAccount {

    @Id
    private String id;

    private int balance;

    @Version
    private long version;

    protected HibAccount() {
    }

    public HibAccount(String id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public String id() {
        return id;
    }

    public int balance() {
        return balance;
    }

    public long version() {
        return version;
    }

    /** Takes {@code amount} out, or refuses: the rule is checked on the state this object was loaded with. */
    public boolean withdraw(int amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }
}
