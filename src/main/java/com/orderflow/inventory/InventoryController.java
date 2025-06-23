package com.orderflow.inventory;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InventoryController {

    private final ProductRepository products;

    InventoryController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping("/inventory")
    List<StockLevel> stock() {
        return products.findAll().stream()
                .map(p -> new StockLevel(p.getSku(), p.getName(), p.getAvailable()))
                .toList();
    }

    record StockLevel(String sku, String name, int available) {
    }
}
