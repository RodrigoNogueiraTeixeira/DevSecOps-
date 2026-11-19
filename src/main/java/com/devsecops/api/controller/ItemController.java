package com.devsecops.api.controller;

import com.devsecops.api.model.Item;
import com.devsecops.api.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "devsecops-spring-api"
        ));
    }

    @GetMapping("/items")
    public ResponseEntity<List<Item>> listItems(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(itemRepository.findByCategoryIgnoreCase(category.trim()));
        }
        return ResponseEntity.ok(itemRepository.findAll());
    }

    @PostMapping("/items")
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item) {
        Item saved = itemRepository.save(new Item(item.getName(), item.getCategory()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
