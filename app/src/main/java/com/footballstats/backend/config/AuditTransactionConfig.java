package com.footballstats.backend.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
/** Transaction advice surrounds audit advice so both commit or roll back together. */
@Configuration
@EnableTransactionManagement(order=0)
public class AuditTransactionConfig {}
