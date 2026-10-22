package com.jk.explore.security.sqlihibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A customer account. The password hash is the thing an injection is after. */
@Entity
@Table(name = "users")
public class Account {

    @Id
    private Long id;
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;

    protected Account() {
    }

    public Account(Long id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }
}
