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
            // If no content exists, create and save a default one.
            return updateHomePageContent("Welcome to the Bible Quiz!");
        }
        return contentList.get(0);
    }

    public HomePageContent updateHomePageContent(String newContent) {
        // Find the first content record, or create a new one if none exist.
        HomePageContent contentToUpdate;
        java.util.Optional<HomePageContent> existingContent = repository.findAll().stream().findFirst();
        if (existingContent.isPresent()) {
            contentToUpdate = existingContent.get();
        } else {
            contentToUpdate = new HomePageContent();
        }
        contentToUpdate.setContent(newContent);
        return repository.save(contentToUpdate);
    }
}