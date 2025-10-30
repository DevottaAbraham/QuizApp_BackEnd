package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import Chruch_Of_God_Dindigul.Bible_quize.service.HomePageContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final HomePageContentService homePageContentService;

    @Autowired
    public ContentController(HomePageContentService homePageContentService) {
        this.homePageContentService = homePageContentService;
    }

    @GetMapping("/home")
    public ResponseEntity<HomePageContent> getHomePageContent() {
        HomePageContent content = homePageContentService.getHomePageContent();
        return ResponseEntity.ok(content);
    }
}