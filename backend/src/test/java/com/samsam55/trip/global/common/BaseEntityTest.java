package com.samsam55.trip.global.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class BaseEntityTest {

    private final EntityManager entityManager;

    @Autowired
    BaseEntityTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    @Transactional
    void 엔티티를_저장하면_공통_필드가_자동으로_기록된다() {
        BaseEntityTestEntity entity = new BaseEntityTestEntity();

        entityManager.persist(entity);
        entityManager.flush();

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }
}
