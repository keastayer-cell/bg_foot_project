package com.footballstats.backend.repository;
import com.footballstats.backend.domain.BusinessAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BusinessAuditRepository extends JpaRepository<BusinessAuditEntry,Long> {}
