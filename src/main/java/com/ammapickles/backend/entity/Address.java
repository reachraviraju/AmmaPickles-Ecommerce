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
    
    @NotBlank(message ="name is required")
    private String name;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    private String district;

    @NotBlank(message = "State is required")
    private String state;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode")
    @Column(length = 6)
    private String pincode;

    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid mobile number")
    @Column(length = 10, nullable = false)
    private String mobileNumber;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}