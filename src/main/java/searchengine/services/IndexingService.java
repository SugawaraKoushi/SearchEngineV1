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
        HashSet<Page> pages = parsePagesFromSite();

        Transaction transaction = session.beginTransaction();
        site.setPageSet(pages);
        session.persist(site);
        transaction.commit();

        insertPagesInDB(pages);

        transaction = session.beginTransaction();
        site.setStatus(Status.INDEXED);
        session.persist(site);
        transaction.commit();

        session.close();
    }

    private void deleteSite() {
        Site site = session.createQuery("from " + Site.class.getSimpleName() + " where name = :nameParam", Site.class)
                .setParameter("nameParam", this.site.getName())
                .getSingleResult();

        Transaction transaction = session.beginTransaction();

        session.createQuery("delete " + Page.class.getSimpleName() + " where site_id = :idParam")
                .setParameter("idParam", site.getId())
                .executeUpdate();

        session.createQuery("delete " + Site.class.getSimpleName() + " where id = :idParam")
                .setParameter("idParam", site.getId())
                .executeUpdate();

        transaction.commit();
    }

    private void createInstanceOfSite() {
        Date date = new Date(System.currentTimeMillis());
        Transaction transaction = session.beginTransaction();

        site.setStatus(Status.INDEXING);
        site.setStatusTime(date);
        site.setLastError(null);
        site.setUrl(this.site.getUrl());
        site.setName(this.site.getName());

        session.persist(site);

        transaction.commit();
    }

    private HashSet<Page> parsePagesFromSite() {
        PageParser pageParser = new PageParser(site);
        HashSet<Page> pages = new ForkJoinPool().invoke(pageParser);
        return pages;
    }

    private void insertPagesInDB(HashSet<Page> set) {
        Transaction transaction = session.beginTransaction();
        for (Page page : set) {
            session.persist(page);
        }
        transaction.commit();
    }
}
