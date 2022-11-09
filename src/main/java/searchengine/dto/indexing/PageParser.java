package searchengine.dto.indexing;

import org.hibernate.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Site;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser extends RecursiveTask<String> {
    private static final Pattern URL_PATTERN = Pattern.compile("(?<homeURL>https?://[^/]+)?(?<else>.+)");

    private static Site site;
    private Page page;

    private Document document;

    public PageParser(Site site) {
        this(site.getUrl());
        PageParser.site = site;
        page = new Page();
    }

    private PageParser(String url) {
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
    protected String compute() {
        ArrayList<PageParser> taskList = new ArrayList<>(); // Список задач
        HashSet<Page> pageSet = handle(page.getPath());  // Список ссылок на текущей странице url

        // Создаем подзадачи для каждой ссылки
        for (Page p : pageSet) {
            PageParser task = new PageParser(p.getPath());
            task.fork();
            taskList.add(task);
        }

        // Суммируем полученные результаты
        StringBuilder builder = new StringBuilder();
        for (PageParser task : taskList) {
            // Разбиваем insert запрос по частям
            if (builder.length() > 3_000_000) {
                insert(builder.substring(1));
                builder = new StringBuilder();
            }
            builder.append(",").append(task.join());
        }

        // Получаем html-код страницы
        String content = document.toString();
        content = content.replaceAll("'", "\\\\'");
        content = content.replaceAll("\"", "\\\\\"");

        return String.format("(\"%s\", %d, \"%s\")", page.getPath(), page.getCode(), page.getContent()) + builder;
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
            page.setCode(response.statusCode());                // код ответа с сервера
            page.setContent(response.parse().toString());       // html-код страницы
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

        session.createQuery("SELECT s.id FROM search_engine.site s WHERE ")

        session.createQuery("INSERT INTO page(`path`, `code`, `content`) VALUES" + values
                + "ON DUPLICATE KEY UPDATE `path` = `path`");

        session.close();
    }
}