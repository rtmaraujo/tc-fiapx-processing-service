package com.fiapx.processing.infrastructure.repository;

import com.fiapx.processing.infrastructure.persistence.ProcessamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessamentoRepository extends JpaRepository<ProcessamentoEntity, UUID> {
}
