package com.jk.explore.security.sqlihibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A product of the online store. Unlisted products are not for sale yet and must never reach a customer. */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private Long id;
    private String name;
    private long price;
    private boolean listed;

    protected Product() {
    }

    public Product(Long id, String name, long price, boolean listed) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.listed = listed;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public boolean isListed() {
        return listed;
    }
}
