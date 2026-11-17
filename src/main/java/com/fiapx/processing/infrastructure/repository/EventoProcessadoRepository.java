package com.fiapx.processing.infrastructure.repository;

import com.fiapx.processing.infrastructure.persistence.EventoProcessadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessadoEntity, String> {
}
