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
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class IndexingService {
    private final Site site = new Site();
    private final Session session;

    public IndexingService(searchengine.config.Site site) {
        this.site.setUrl(site.getUrl());
        this.site.setName(site.getName());
        session = SQLQueryExecutor.createSession();
    }

    public int index() {
        deleteSite();
        createInstanceOfSite();
        HashSet<Page> pages = parsePagesFromSite();

        if (isSiteStatusFailed()) {
            session.close();
            return -1;
        }

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
        return 0;
    }

    // Удаляет сущность site из базы данных
    private void deleteSite() {
        List<Site> siteList = session.createQuery("from " + Site.class.getSimpleName() + " where name = :nameParam", Site.class)
                .setParameter("nameParam", this.site.getName())
                .getResultList();

        if (siteList.isEmpty()) {
            return;
        }

        Transaction transaction = session.beginTransaction();

        session.createQuery("delete " + Page.class.getSimpleName() + " where site_id = :idParam")
                .setParameter("idParam", siteList.get(0).getId())
                .executeUpdate();

        session.createQuery("delete " + Site.class.getSimpleName() + " where id = :idParam")
                .setParameter("idParam", siteList.get(0).getId())
                .executeUpdate();

        transaction.commit();
    }

    // Добавляет сущность site в базу данных
    private void createInstanceOfSite() {
        List<Site> siteList = session.createQuery("from " + Site.class.getSimpleName() + " where url = :urlParam", Site.class)
                .setParameter("urlParam", this.site.getUrl())
                .getResultList();

        if (!siteList.isEmpty()) {
            return;
        }

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

    // Возвращает множество страниц с сайта site
    private HashSet<Page> parsePagesFromSite() {
        PageParser pageParser = new PageParser(site);
        HashSet<Page> pages = new ForkJoinPool().invoke(pageParser);
        return pages;
    }


    // Вносит странцы с сайта site в базу данных
    private void insertPagesInDB(HashSet<Page> set) {
        Transaction transaction = session.beginTransaction();
        for (Page page : set) {
            session.persist(page);
        }
        transaction.commit();
    }

    private boolean isSiteStatusFailed() {
        List<Site> siteList = session.createQuery("from " + Site.class.getSimpleName()
                        + " where id = :idParam and status = :statusParam", Site.class)
                .setParameter("idParam", this.site.getId())
                .setParameter("statusParam", Status.FAILED)
                .getResultList();

        return !siteList.isEmpty();
    }
}
