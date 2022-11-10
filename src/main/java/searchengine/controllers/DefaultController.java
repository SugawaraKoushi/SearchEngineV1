package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.dto.indexing.PageParser;
import searchengine.model.Site;

import java.util.concurrent.ForkJoinPool;

@Controller
public class DefaultController {

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
        return "start";
    }
}
