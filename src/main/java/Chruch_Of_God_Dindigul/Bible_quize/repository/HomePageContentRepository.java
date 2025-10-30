package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HomePageContentRepository extends JpaRepository<HomePageContent, Long> {
    // This method is designed to retrieve the single home page content entry.
    // We assume there will only be one, so we fetch the top one ordered by ID.
    Optional<HomePageContent> findTopByOrderByIdAsc();
}