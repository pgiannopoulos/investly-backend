package com.investly.repositories;

import com.investly.entities.MaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaskRepository extends JpaRepository<MaskEntity, Integer> {
}
