package searchengine.dto.indexing;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Site;
import searchengine.model.Page;
import searchengine.model.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser extends RecursiveTask<HashSet<Page>> {
    private static final Pattern URL_PATTERN = Pattern.compile("(?<homeURL>https?://[^/]+)?(?<else>.+)");

    private static Site site;
    private Page page = new Page();

    public PageParser(Site site) {
        this(site.getUrl());
        this.site = site;
        page.setSite(site);
    }

    private PageParser(String url) {
        page.setSite(site);
        // Проверяем, стоит ли в конце символ "/"
        if (!url.endsWith("/") && !url.endsWith(".html")) {
            url = url.concat("/");
        }

        Matcher matcher = URL_PATTERN.matcher(url);

        if (matcher.find()) {
            page.setPath(matcher.group("else"));
        }
    }

    @Override
    protected HashSet<Page> compute() {
        ArrayList<PageParser> taskList = new ArrayList<>(); // Список задач
        HashSet<Page> pageSet = handle(page.getPath());  // Список ссылок на текущей странице url
        HashSet<Page> resultSet = new HashSet<>();

        if (pageSet == null) {
            return null;
        }

        // Создаем подзадачи для каждой ссылки
        for (Page p : pageSet) {
            PageParser task = new PageParser(p.getPath());
            task.fork();
            taskList.add(task);
        }

        resultSet.add(page);
        for (PageParser task : taskList) {
            resultSet.add(task.page);
            HashSet<Page> result = task.join();
            for (Page p : result) {
                resultSet.add(p);
            }
        }

        return resultSet;
    }

    // Возвращает множество страниц со страницы url
    private HashSet<Page> handle(String url) {
        HashSet<String> urlSet = new HashSet<>();
        HashSet<Page> pageSet = new HashSet<>();

        try {
            // Подключаемся к сайту
            org.jsoup.Connection.Response response = Jsoup.connect(site.getUrl() + url)
                    .userAgent("BobSearcherBot")
                    .referrer("http://www.google.com")
                    .execute();

            updateSiteStatusTime();

            TimeUnit.MILLISECONDS.sleep(50);

            Document document = response.parse();
            page.setCode(response.statusCode());    // код ответа с сервера

            // html-код страницы
            String content = document.toString();
            content = content.replaceAll("'", "\\\\'");
            content = content.replaceAll("\"", "\\\\\"");
            page.setContent(content);

            Elements elements = document.select("a");  // гиперссылки

            // Получаем ссылки
            elements.forEach(element -> urlSet.add(element.attr("href")));

            // Удаляем все, что не является ссылками, а так же ссылку на текущую и на домашнюю страницы
            urlSet.removeIf(
                    href -> !href.startsWith(page.getPath()) || href.equals(page.getPath()) || href.contains("#")
            );

            // Создаем страницы на основе ссылок
            for (String path : urlSet) {
                Page p = new Page();
                p.setPath(path);
                pageSet.add(p);
            }
        } catch (Exception exception) {
            Session session = SQLQueryExecutor.createSession();
            Transaction transaction = session.beginTransaction();
            site.setStatus(Status.FAILED);
            site.setLastError(exception.getLocalizedMessage());
            transaction.commit();
            session.close();

            return null;
        }

        return pageSet;
    }

    // Вносит все страницы в базу данных


    private void updateSiteStatusTime() {
        Session session = SQLQueryExecutor.createSession();
        Transaction transaction = session.beginTransaction();
        Date now = new Date(System.currentTimeMillis());
        site.setStatusTime(now);
        session.merge(site);
        transaction.commit();
        session.close();
    }

}