package com.minicommerce.orders.repository;

import com.minicommerce.orders.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> { }
