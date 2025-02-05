package com.investly.app;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaskRepository extends JpaRepository<MaskEntity, Integer> {
}
