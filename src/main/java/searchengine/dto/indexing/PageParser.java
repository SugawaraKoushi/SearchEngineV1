package searchengine.dto.indexing;

import org.hibernate.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Site;
import searchengine.model.Page;
import searchengine.model.Status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser extends RecursiveTask<HashSet<Page>> {
    private static final Pattern URL_PATTERN = Pattern.compile("(?<homeURL>https?://[^/]+)?(?<else>.+)");
    private static HashSet pages = new HashSet<>();

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

        // Создаем подзадачи для каждой ссылки
        for (Page p : pageSet) {
            PageParser task = new PageParser(p.getPath());
            task.fork();
            taskList.add(task);
        }

        for (PageParser task : taskList) {
            pages.add(task.page);
        }

        return pages;
    }

    // Возвращает список страниц со страницы url
    private HashSet<Page> handle(String url) {
        HashSet<String> urlSet = new HashSet<>();
        HashSet<Page> pageSet = new HashSet<>();

        try {
            // Подключаемся к сайту
            org.jsoup.Connection.Response response = Jsoup.connect(site.getUrl() + url)
                    .userAgent("BobSearcherBot")
                    .referrer("http://www.google.com")
                    .execute();

            TimeUnit.MILLISECONDS.sleep(50);

            Document document = response.parse();
            page.setCode(response.statusCode());    // код ответа с сервера

            // html-код страницы
            String content = document.toString();
            content = content.replaceAll("'", "\\\\'");
            content = content.replaceAll("\"", "\\\\\"");
            page.setContent(document.toString());

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
            exception.printStackTrace();
        }

        return pageSet;
    }

    // Вносим данные в базу данных
    public void insert(String values) {
        Session session = SQLQueryExecutor.createSession();

        session.createQuery("INSERT INTO page(`site_id`, `path`, `code`, `content`) VALUES" + values);

        session.close();
    }

    private void setSiteStatus(Status status) {
        if (site.getStatus() == status) {
            return;
        }

        Session session = SQLQueryExecutor.createSession();
        String hql = String.format("""
                UPDATE site
                SET
                    site.status = '%s'
                WHERE
                    site.name = '%s'
                """, status.name(), site.getName());
        session.createQuery(hql);
        session.close();
    }

}