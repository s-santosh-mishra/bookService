package com.servicehub.serv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SettlementTimeConfiguration {

    @Bean
    public Clock settlementClock() {
        return Clock.systemUTC();
    }
}
