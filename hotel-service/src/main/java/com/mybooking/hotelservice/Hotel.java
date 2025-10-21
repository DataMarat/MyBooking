package com.mybooking.hotelservice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotels")
@Data                      // генерирует геттеры/сеттеры, toString, equals/hashCode
@NoArgsConstructor          // нужен JPA
@AllArgsConstructor         // удобно для тестов и предзаполнения
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;
}
