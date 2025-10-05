package com.stan.stancore.extended.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories({ "com.systemspecs.remita.vending.repository" })
@EnableTransactionManagement
public class ExtendedDatabaseConfiguration {}
