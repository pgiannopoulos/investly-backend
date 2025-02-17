package com.investly.app.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Optional<MessageEntity> findFirstByMaskEntityOrderByTimestampDesc(MaskEntity maskEntity);
}

