package com.ctf.dto;

import com.ctf.dto.hint.HintDTO;
import com.ctf.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Integer id;
    private String title;
    private String description;
    private String fileUrl;
    private Integer categoryId;
    private Integer points;
    private String difficulty;
    private List<HintDTO> hints;

    public static QuestionResponse from(Question question) {
        if (question == null) {
            return null;
        }
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setDescription(question.getDescription());
        response.setFileUrl(question.getFileUrl());
        response.setCategoryId(question.getCategoryId());
        response.setPoints(question.getPoints());
        response.setDifficulty(question.getDifficulty());
        return response;
    }
}
