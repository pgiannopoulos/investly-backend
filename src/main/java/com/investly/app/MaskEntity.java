package com.investly.app;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "masks")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class MaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String label;
}

