package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.Cart;
import com.ammapickles.backend.entity.CartItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface CartRepository extends JpaRepository<Cart, Long> {

	 Optional<Cart> findByUserId(Long userId);  // fetch user's cart
	    void deleteByUserId(Long userId);          //to clear cart when order placed
}
