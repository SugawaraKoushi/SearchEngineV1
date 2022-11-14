package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.ResponseAnswer;
import searchengine.services.IndexingService;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
@RequiredArgsConstructor
public class DefaultController {
    private final SitesList sites;

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
    @ResponseBody
    public ResponseAnswer startIndexing() {
        String text = "";

            IndexingService indexingService = new IndexingService(sites.getSites().get(0));
            indexingService.index();
            text = indexingService.index() == 0 ? "{'result': true}" : "{'result': false,'error': \"Индексация уже запущена\"}";


        return new ResponseAnswer(text);
    }
}
