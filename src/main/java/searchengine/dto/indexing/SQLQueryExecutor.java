package searchengine.dto.indexing;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class SQLQueryExecutor {
    public static Session createSession() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure("application.yaml").build();
        Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
        SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
        return sessionFactory.openSession();
    }
}