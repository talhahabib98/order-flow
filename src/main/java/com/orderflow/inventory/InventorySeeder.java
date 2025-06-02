package com.orderflow.inventory;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds a small demo catalogue so the API can be tried straight away. */
@Component
class InventorySeeder implements ApplicationRunner {

    private final ProductRepository products;

    InventorySeeder(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (products.count() == 0) {
            products.saveAll(List.of(
                    new Product("BOOK-001", "Paperback book", 100),
                    new Product("PEN-001", "Ballpoint pen", 1000),
                    new Product("LAPTOP-001", "Laptop", 5)));
        }
    }
}
