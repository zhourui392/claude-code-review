package com.example.gitreview.infrastructure.storage.config;

import org.springframework.context.annotation.Configuration;

/**
 * Storage Configuration
 * Storage adapters are directly annotated with @Component and implement repository interfaces
 * No additional bean configuration needed - Spring will automatically wire the implementations
 */
@Configuration
public class StorageConfiguration {
    // All storage adapters are @Component beans that implement repository interfaces
    // Spring will automatically inject them where needed
}