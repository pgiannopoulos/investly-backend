<<<<<<<< HEAD:src/main/java/com/investly/app/dao/MessageRepository.java
package com.investly.app.dao;

import com.investly.app.dao.MessageEntity;
========
package com.investly.repositories;

import com.investly.entities.MessageEntity;
>>>>>>>> e3137d2 (Split into folders and changed db1.sql):src/main/java/com/investly/repositories/MessageRepository.java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Optional<MessageEntity> findFirstByOrderByTimestampDesc();
}
