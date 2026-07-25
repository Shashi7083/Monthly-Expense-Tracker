package com.shashi.sms.config;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MongoJobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MongoJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MongoDBConfig {

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        MongoTemplate mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
        MappingMongoConverter mappingMongoConverter = (MappingMongoConverter) mongoTemplate.getConverter();
        mappingMongoConverter.setMapKeyDotReplacement("_");
        mappingMongoConverter.afterPropertiesSet();
        return mongoTemplate;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new ResourcelessTransactionManager();
    }

    @Bean
    public JobRepository jobRepository(MongoTemplate mongoTemplate, PlatformTransactionManager transactionManager) throws Exception {
        MongoJobRepositoryFactoryBean jobRepositoryFactoryBean = new MongoJobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setMongoOperations(mongoTemplate);
        jobRepositoryFactoryBean.setTransactionManager(transactionManager);
        jobRepositoryFactoryBean.afterPropertiesSet();
        return jobRepositoryFactoryBean.getObject();
    }

    @Bean
    public JobExplorer jobExplorer(MongoTemplate mongoTemplate, PlatformTransactionManager transactionManager) throws Exception {
        MongoJobExplorerFactoryBean mongoExplorerFactoryBean = new MongoJobExplorerFactoryBean();
        mongoExplorerFactoryBean.setMongoOperations(mongoTemplate);
        mongoExplorerFactoryBean.setTransactionManager(transactionManager);
        mongoExplorerFactoryBean.afterPropertiesSet();
        return mongoExplorerFactoryBean.getObject();
    }
}
