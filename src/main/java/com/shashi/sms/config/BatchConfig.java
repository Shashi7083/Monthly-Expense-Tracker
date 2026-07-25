package com.shashi.sms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.shashi.sms.listener.SmsMessageListener;
import com.shashi.sms.mapper.SMSMessageMapper;
import com.shashi.sms.model.BankTransaction;
import com.shashi.sms.model.SMSMessage;
import com.shashi.sms.processor.SMSMessageProcessor;
import com.shashi.sms.writer.SMSMessageWriter;

@Configuration
public class BatchConfig {

    private JobRepository jobRepository;
    private PlatformTransactionManager transactionManager;

    public BatchConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public JobLauncher jobLauncher() throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public static org.springframework.batch.core.scope.StepScope stepScope() {
        org.springframework.batch.core.scope.StepScope stepScope = new org.springframework.batch.core.scope.StepScope();
        stepScope.setAutoProxy(true);
        return stepScope;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<SMSMessage> smsMessageFlatFileItemReader(
            @Value("#{jobParameters['inputFile']}") String inputFilePath) {

        FlatFileItemReader<SMSMessage> itemReader = new FlatFileItemReader<>();
        itemReader.setLinesToSkip(1);
        if (inputFilePath != null && !inputFilePath.trim().isEmpty()) {
            itemReader.setResource(new FileSystemResource(inputFilePath));
        } else {
            itemReader.setResource(new ClassPathResource("sms_report.txt"));
        }
        DefaultLineMapper<SMSMessage> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter("|");
        tokenizer.setNames("Sender", "Date", "Read", "Type", "Thread", "Service", "Message");
        tokenizer.setStrict(false);
        tokenizer.setIncludedFields(new int[] { 0, 1, 2, 3, 4, 5, 6 });
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new SMSMessageMapper());
        itemReader.setLineMapper(lineMapper);
        return itemReader;
    }

    @Bean
    public ItemProcessor<SMSMessage, BankTransaction> smsMessageProcessor() {
        return new SMSMessageProcessor();
    }

    @Bean
    public ItemWriter<BankTransaction> smsMessageWriter() {
        return new SMSMessageWriter();
    }

    @Bean
    public SmsMessageListener smsMessageListener() {
        return new SmsMessageListener();
    }

    @Bean(name = "smsMessageStep")
    public Step smsMessageStep(ItemReader<SMSMessage> smsMessageFlatFileItemReader) {
        return new StepBuilder("smsMessageStep", jobRepository)
                .<SMSMessage, BankTransaction>chunk(100, transactionManager)
                .reader(smsMessageFlatFileItemReader)
                .processor(smsMessageProcessor())
                .writer(smsMessageWriter())
                .taskExecutor(taskExecutor())
                .throttleLimit(4)
                .build();
    }

    @Bean(name = "smsMessageJob")
    public Job smsMessageJob(Step smsMessageStep) {
        return new JobBuilder("smsMessageJob", jobRepository)
                .listener(smsMessageListener())
                .incrementer(new RunIdIncrementer())
                .start(smsMessageStep)
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("batch-thread-");
    }

}
