package com.businessos.shared.address;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String country;
    private String level1;
    private String level2;
    private String level3;
    private String level4;
    
    private String postalCode;
    private String streetAddress;
    private String apartment;
}
