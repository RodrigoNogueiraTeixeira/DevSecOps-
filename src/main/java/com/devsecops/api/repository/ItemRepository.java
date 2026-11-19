package com.devsecops.api.repository;

import com.devsecops.api.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // Consulta parametrizada segura do Spring Data JPA (previne SQL Injection)
    List<Item> findByCategoryIgnoreCase(String category);
}
