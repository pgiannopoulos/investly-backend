package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "masks")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return description;
    }

    public void setLabel(String label) {
        this.description = description;
    }
}

