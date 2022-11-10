package searchengine.services;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.dto.indexing.SQLQueryExecutor;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.Date;

public class IndexingService {
    private final Site site = new Site();
    private final Session session;

    public IndexingService(searchengine.config.Site site) {
        site.setUrl(site.getUrl());
        site.setName(site.getName());
        session = SQLQueryExecutor.createSession();
    }

    public void index() {
        delete();
        createInstanceOfSite();

    }

    private void delete() {
        session.createQuery("DELETE * FROM search_engine.site s WHERE s.url = " + site.getUrl());
    }

    private void createInstanceOfSite() {
        Date date = new Date(System.currentTimeMillis());
        Transaction transaction = session.getTransaction();

        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setStatusTime(date);
        site.setLastError(null);
        site.setUrl(this.site.getUrl());
        site.setName(this.site.getName());

        session.persist(site);

        transaction.commit();
        session.close();
    }
}
