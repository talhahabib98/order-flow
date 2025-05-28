package com.orderflow.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String sku;

    private String name;

    private int available;

    protected Product() {
    }

    public Product(String sku, String name, int available) {
        this.sku = sku;
        this.name = name;
        this.available = available;
    }

    boolean canFulfil(int quantity) {
        return available >= quantity;
    }

    void take(int quantity) {
        available -= quantity;
    }

    void giveBack(int quantity) {
        available += quantity;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public int getAvailable() {
        return available;
    }
}
