package searchengine.services;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.dto.indexing.PageParser;
import searchengine.dto.indexing.SQLQueryExecutor;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

public class IndexingService {
    private final Site site = new Site();
    private final Session session;

    public IndexingService(searchengine.config.Site site) {
        this.site.setUrl(site.getUrl());
        this.site.setName(site.getName());
        session = SQLQueryExecutor.createSession();
    }

    public void index() {
        deleteSite();
        createInstanceOfSite();

        PageParser pageParser = new PageParser(site);
        HashSet<Page> pages = new ForkJoinPool().invoke(pageParser);
        System.out.println(pages.size());
        session.close();
    }

    private void deleteSite() {
        String hql = String.format("DELETE FROM %s WHERE name = '%s'", Site.class.getSimpleName(),  site.getName());
        Transaction transaction = session.beginTransaction();
        session.createQuery(hql).executeUpdate();
        transaction.commit();
    }

    private void createInstanceOfSite() {
        Date date = new Date(System.currentTimeMillis());
        Transaction transaction = session.beginTransaction();

        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setStatusTime(date);
        site.setLastError(null);
        site.setUrl(this.site.getUrl());
        site.setName(this.site.getName());

        session.persist(site);

        transaction.commit();
    }
}
