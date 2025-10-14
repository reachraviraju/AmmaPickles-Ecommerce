package com.ammapickles.backend.entity;
import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private double distanceInKm;

   
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
