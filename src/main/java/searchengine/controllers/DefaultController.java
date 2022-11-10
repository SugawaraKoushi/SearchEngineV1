package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.config.Site;
import searchengine.services.IndexingService;

import java.util.concurrent.ForkJoinPool;

@Controller
public class DefaultController {
    private IndexingService indexingService;

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public String startIndexing() {
        Site site = new Site();
        site.setName("PlayBack");
        site.setUrl("http://www.playback.ru");

        indexingService = new IndexingService(site);
        indexingService.index();

        return "index";
    }
}
