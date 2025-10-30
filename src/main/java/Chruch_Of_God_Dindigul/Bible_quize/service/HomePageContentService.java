package Chruch_Of_God_Dindigul.Bible_quize.service;

import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import Chruch_Of_God_Dindigul.Bible_quize.repository.HomePageContentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomePageContentService {

    private final HomePageContentRepository homePageContentRepository;

    @Autowired
    public HomePageContentService(HomePageContentRepository homePageContentRepository) {
        this.homePageContentRepository = homePageContentRepository;
    }

    @Transactional
    @CacheEvict(value = "homeContent", allEntries = true) // Invalidate the cache on update
    public HomePageContent updateHomePageContent(String newContent) {
        // Retrieve the single HomePageContent entry.
        // If it doesn't exist, create a new one.
        HomePageContent homePageContent = homePageContentRepository.findTopByOrderByIdAsc()
                                            .orElseGet(HomePageContent::new);

        homePageContent.setContent(newContent);
        return homePageContentRepository.save(homePageContent);
    }

    @Transactional(readOnly = true)
    @Cacheable("homeContent") // Cache the result of this method
    public HomePageContent getHomePageContent() {
        // Retrieve the single HomePageContent entry.
        // If it doesn't exist, return a default empty one.
        return homePageContentRepository.findTopByOrderByIdAsc()
                                            .orElseGet(() -> new HomePageContent(null, "Welcome to the Quiz App!"));
    }
}