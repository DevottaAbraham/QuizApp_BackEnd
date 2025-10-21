package Chruch_Of_God_Dindigul.Bible_quize.service;

import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import Chruch_Of_God_Dindigul.Bible_quize.repository.HomePageContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HomePageContentService {

    private final HomePageContentRepository repository;

    @Autowired
    public HomePageContentService(HomePageContentRepository repository) {
        this.repository = repository;
    }

    public HomePageContent getHomePageContent() {
        List<HomePageContent> contentList = repository.findAll();
        if (contentList.isEmpty()) {
            HomePageContent defaultContent = new HomePageContent();
            defaultContent.setContent("Welcome to the Bible Quiz!");
            return defaultContent;
        }
        return contentList.get(0);
    }

    public HomePageContent updateHomePageContent(String newContent) {
        List<HomePageContent> contentList = repository.findAll();
        HomePageContent contentToUpdate;
        if (contentList.isEmpty()) {
            contentToUpdate = new HomePageContent();
        } else {
            contentToUpdate = contentList.get(0);
        }
        contentToUpdate.setContent(newContent);
        return repository.save(contentToUpdate);
    }
}