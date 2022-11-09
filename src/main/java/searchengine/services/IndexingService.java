package searchengine.services;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.dto.indexing.SQLQueryExecutor;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.util.Date;

public class IndexingService {
    @Autowired
    private SiteRepository siteRepository;

    private final Site site = new Site();
    private final Session session;

    public IndexingService(searchengine.config.Site site) {
        site.setUrl(site.getUrl());
        site.setName(site.getName());
        session = SQLQueryExecutor.createSession();
    }

    public void index() {
        delete();
        createInstance();

    }

    private void delete() {
        session.createQuery("DELETE * FROM search_engine.site s WHERE s.url = " + site.getUrl());
    }

    private void createInstance() {
        Date date = new Date(System.currentTimeMillis());
        session.createQuery(String.format("""
                INSERT site(id, status, status_time, last_error, url, name)
                VALUES ('%s', '%s', '%s', '%s', '%s')
                """, Status.INDEXING, date, null, site.getUrl(), site.getName()));
    }

    private void updateStatus(Status status) {
        session.createQuery("UPDATE search_engine.site SET site.status = " + status.name() +
                " WHERE site.url = " + site.getUrl());
    }

}
