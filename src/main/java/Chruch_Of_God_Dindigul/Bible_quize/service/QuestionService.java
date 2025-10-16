package Chruch_Of_God_Dindigul.Bible_quize.service;

import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Question;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.repository.QuestionRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    private final Gson gson = new Gson();
    private final Type listStringType = new TypeToken<List<String>>() {}.getType();

    public QuestionDTO createQuestion(QuestionDTO dto, User author) {
        Question question = convertToEntity(dto);
        // The status for a new question is always "draft".
        question.setStatus("draft");
        question.setAuthor(author);
        Question savedQuestion = questionRepository.save(question);
        return convertToDto(savedQuestion);
    }

    public List<QuestionDTO> getAllQuestions() {
        return questionRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public long getQuestionCount() {
        return questionRepository.count();
    }

    public long getPublishedQuestionCount() {
        return questionRepository.countByStatus("published");
    }

    public Optional<QuestionDTO> getQuestionById(Long id) {
        return questionRepository.findById(id).map(this::convertToDto);
    }

    public QuestionDTO updateQuestion(Long id, QuestionDTO dto) {
        Question existingQuestion = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        // Use the converter to get a temporary entity with the new data
        Question updatedData = convertToEntity(dto);

        // Manually update only the fields that should be editable
        existingQuestion.setText_en(updatedData.getText_en());
        existingQuestion.setOptions_en(updatedData.getOptions_en());
        existingQuestion.setCorrectAnswer_en(updatedData.getCorrectAnswer_en());
        existingQuestion.setText_ta(updatedData.getText_ta());
        existingQuestion.setOptions_ta(updatedData.getOptions_ta());
        existingQuestion.setCorrectAnswer_ta(updatedData.getCorrectAnswer_ta());
        // Note: We are intentionally NOT updating the author or status here.
        // Status changes (like publishing) should be handled by a separate, more specific method.

        return convertToDto(questionRepository.save(existingQuestion));
    }

    public QuestionDTO publishQuestion(Long id, String releaseDate, String disappearDate) {
        Question existingQuestion = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        existingQuestion.setStatus("published");
        existingQuestion.setReleaseDate(releaseDate);
        existingQuestion.setDisappearDate(disappearDate);

        Question publishedQuestion = questionRepository.save(existingQuestion);
        return convertToDto(publishedQuestion);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    private QuestionDTO convertToDto(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setText_en(question.getText_en());
        dto.setOptions_en(gson.fromJson(question.getOptions_en(), listStringType));
        dto.setCorrectAnswer_en(question.getCorrectAnswer_en()); // Send the answer text directly
        dto.setText_ta(question.getText_ta());
        dto.setOptions_ta(gson.fromJson(question.getOptions_ta(), listStringType));
        dto.setCorrectAnswer_ta(question.getCorrectAnswer_ta()); // Send the answer text directly
        dto.setStatus(question.getStatus());
        dto.setReleaseDate(question.getReleaseDate());
        dto.setDisappearDate(question.getDisappearDate());
        if (question.getAuthor() != null) {
            dto.setAuthorUsername(question.getAuthor().getUsername());
        }
        return dto;
    }

    private Question convertToEntity(QuestionDTO dto) {
        Question question = new Question();
        question.setId(dto.getId());

        // --- LOGIC CORRECTION: Process the answer as a string, not an index ---
        if (dto.getOptions_en() == null || dto.getOptions_en().isEmpty()) {
            throw new IllegalArgumentException("English options cannot be empty.");
        }
        if (dto.getCorrectAnswer_en() == null || !dto.getOptions_en().contains(dto.getCorrectAnswer_en())) {
            throw new IllegalArgumentException("The provided English correct answer is not one of the available options.");
        }

        if (dto.getOptions_ta() == null || dto.getOptions_ta().isEmpty()) {
            throw new IllegalArgumentException("Tamil options cannot be empty.");
        }
        if (dto.getCorrectAnswer_ta() == null || !dto.getOptions_ta().contains(dto.getCorrectAnswer_ta())) {
            throw new IllegalArgumentException("The provided Tamil correct answer is not one of the available options.");
        }

        // CRITICAL FIX: Set the question text for both English and Tamil.
        question.setText_en(dto.getText_en()); 
        question.setOptions_en(gson.toJson(dto.getOptions_en()));
        question.setCorrectAnswer_en(dto.getCorrectAnswer_en());
        question.setText_ta(dto.getText_ta()); 
        question.setOptions_ta(gson.toJson(dto.getOptions_ta()));
        question.setCorrectAnswer_ta(dto.getCorrectAnswer_ta());

        // CRITICAL FIX: Map the status from the DTO to the entity.
        question.setStatus(dto.getStatus());
        question.setReleaseDate(dto.getReleaseDate());
        question.setDisappearDate(dto.getDisappearDate());
        
        return question;
    }

    public void publishBulkQuestions(List<Long> questionIds, String releaseDate, String disappearDate) {
        List<Question> questionsToPublish = questionRepository.findAllById(questionIds);
        for (Question question : questionsToPublish) {
            question.setStatus("published");
            question.setReleaseDate(releaseDate);
            question.setDisappearDate(disappearDate);
        }
        questionRepository.saveAll(questionsToPublish);
    }

    public void deleteAllPublishedQuestions() {
        List<Question> publishedQuestions = questionRepository.findByStatus("published");
        if (publishedQuestions != null && !publishedQuestions.isEmpty()) {
            questionRepository.deleteAll(publishedQuestions);
        }
    }
}
