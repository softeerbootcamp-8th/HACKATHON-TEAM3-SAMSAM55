package com.samsam55.trip.global.common;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "base_entity_test")
public class BaseEntityTestEntity extends BaseEntity {

    protected BaseEntityTestEntity() {
    }
}
