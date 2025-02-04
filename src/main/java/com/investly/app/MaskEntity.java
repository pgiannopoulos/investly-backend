package com.investly.app;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "masks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String label;

    @OneToMany(mappedBy = "maskEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude  // Prevents infinite recursion in bidirectional relationships
    private List<MessageEntity> messages;
}

