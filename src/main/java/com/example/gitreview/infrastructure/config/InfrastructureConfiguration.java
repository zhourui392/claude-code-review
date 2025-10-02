package com.example.gitreview.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure Configuration
 * Infrastructure adapters are directly annotated with @Component and implement port interfaces
 * No additional bean configuration needed - Spring will automatically wire the implementations
 */
@Configuration
public class InfrastructureConfiguration {
    // All infrastructure adapters are @Component beans that implement port interfaces
    // Spring will automatically inject them where needed
}
