package be.abis.sandwich.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    /**
     * Runs code in a transaction with isolation level SERIALIZABLE: the database makes sure that transactions
     * that run at the same time give the same result as if they ran one after the other. When that is not possible
     * it makes one of them fail, so the caller has to retry (see AbisSandwichOrderService).
     */
    @Bean
    public TransactionOperations serializableTransactions(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        return template;
    }
}
