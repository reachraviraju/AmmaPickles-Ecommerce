package com.ammapickles.backend.dto.address;

import lombok.Data;

@Data
public class AddressResponse {

    private Long id;
    private String street;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private Double distanceInKm;
    private String formattedAddress;  // "street, city, district - pincode"
}
