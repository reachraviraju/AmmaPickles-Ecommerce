package com.ammapickles.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    private String district;

    @NotBlank(message = "State is required")
    private String state;

              //  must be exactly 6 digits
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode")
    @Column(length = 6)
    private String pincode;

    
    @Column(nullable = false)
    private double distanceInKm;

 
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}