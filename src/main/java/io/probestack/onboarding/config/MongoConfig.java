package io.probestack.onboarding.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "io.probestack.onboarding.repository")
public class MongoConfig {
}
