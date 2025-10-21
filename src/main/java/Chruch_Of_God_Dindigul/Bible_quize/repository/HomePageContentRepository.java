package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageContentRepository extends JpaRepository<HomePageContent, Long> {}