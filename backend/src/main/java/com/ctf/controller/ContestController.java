package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.ContestStatusResponse;
import com.ctf.dto.QuestionResponse;
import com.ctf.dto.RankingItem;
import com.ctf.dto.SubmitAnswerRequest;
import com.ctf.dto.hint.HintDTO;
import com.ctf.entity.Announcement;
import com.ctf.entity.ContestUser;
import com.ctf.entity.Question;
import com.ctf.service.AnnouncementService;
import com.ctf.service.AuthService;
import com.ctf.service.ContestService;
import com.ctf.service.HintService;
import com.ctf.util.ContestTimeUtil;
import com.ctf.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/contest")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class ContestController {

    @Autowired
    private ContestService contestService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ContestTimeUtil contestTimeUtil;

    @Autowired
    private HintService hintService;

    @Autowired
    private AnnouncementService announcementService;

    private Integer getUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenUtil.getUserIdFromToken(token);
    }

    @GetMapping("/status")
    public ApiResponse<ContestStatusResponse> getContestStatus() {
        ContestTimeUtil.ContestStatus status = contestTimeUtil.getCurrentStatus();
        Long remainingTime = contestTimeUtil.getRemainingTimeMillis();
        Boolean canStart = contestTimeUtil.canStartContest();
        Boolean isActive = contestTimeUtil.isContestActive();

        ContestStatusResponse response = new ContestStatusResponse();
        response.setStatus(status.toString());
        response.setRemainingTime(remainingTime);
        response.setCanStartContest(canStart);
        response.setContestActive(isActive);

        switch (status) {
            case NOT_STARTED:
                response.setMessage("比赛未开始，敬请期待");
                break;
            case READY:
                response.setMessage("准备阶段，即将开始比赛");
                break;
            case RUNNING:
                response.setMessage("比赛进行中");
                break;
            case FINISHED:
                response.setMessage("比赛已结束");
                break;
            case RESULTS_PUBLISHED:
                response.setMessage("成绩已公布");
                break;
        }

        return ApiResponse.success(response);
    }

    @PostMapping("/start")
    public ApiResponse<String> startContest(@RequestHeader("Authorization") String token) {
        Integer userId = getUserIdFromToken(token);

        if (!contestTimeUtil.canStartContest()) {
            throw new IllegalArgumentException("Contest has not started yet");
        }

        contestService.initializeContestForUser(userId);
        log.info("Contest started for user: userId={}", userId);
        return ApiResponse.success(null, "Contest started");
    }

    @GetMapping("/current-question")
    public ApiResponse<QuestionResponse> getCurrentQuestion(@RequestHeader("Authorization") String token) {
        Integer userId = getUserIdFromToken(token);

        Question question = contestService.getCurrentQuestion(userId);
        if (question == null) {
            throw new IllegalArgumentException("No question found");
        }

        QuestionResponse response = QuestionResponse.from(question);
        List<HintDTO> hints = hintService.getHintsWithUnlockStatus(question.getId(), userId);
        response.setHints(hints);

        return ApiResponse.success(response);
    }

    @GetMapping("/questions")
    public ApiResponse<List<QuestionResponse>> getSelectedQuestions(@RequestHeader("Authorization") String token) {
        Integer userId = getUserIdFromToken(token);

        ContestUser contestUser = contestService.getContestUserByUserId(userId);
        if (contestUser == null) {
            throw new IllegalArgumentException("User not in contest");
        }

        List<Integer> selectedIds = contestService.getSelectedQuestions(contestUser);
        List<QuestionResponse> responses = new ArrayList<>();

        for (Integer qId : selectedIds) {
            Question question = contestService.getQuestionById(qId);
            if (question != null) {
                responses.add(QuestionResponse.from(question));
            }
        }

        return ApiResponse.success(responses);
    }

    @PostMapping("/submit-answer")
    public ApiResponse<Boolean> submitAnswer(@RequestHeader("Authorization") String token,
                                            @RequestBody SubmitAnswerRequest request) {
        Integer userId = getUserIdFromToken(token);

        if (!contestTimeUtil.isContestActive()) {
            throw new IllegalArgumentException("Contest is not active");
        }

        boolean isCorrect = contestService.submitAnswer(userId, request.getQuestionId(), request.getAnswer());
        return ApiResponse.success(isCorrect, isCorrect ? "Correct answer" : "Wrong answer");
    }

    @PostMapping("/next-question")
    public ApiResponse<QuestionResponse> nextQuestion(@RequestHeader("Authorization") String token) {
        Integer userId = getUserIdFromToken(token);

        contestService.moveToNextQuestion(userId);
        Question question = contestService.getCurrentQuestion(userId);

        if (question == null) {
            throw new IllegalArgumentException("No more questions");
        }

        return ApiResponse.success(QuestionResponse.from(question));
    }

    @PostMapping("/prev-question")
    public ApiResponse<QuestionResponse> prevQuestion(@RequestHeader("Authorization") String token) {
        Integer userId = getUserIdFromToken(token);

        contestService.moveToPreviousQuestion(userId);
        Question question = contestService.getCurrentQuestion(userId);

        if (question == null) {
            throw new IllegalArgumentException("No previous question");
        }

        return ApiResponse.success(QuestionResponse.from(question));
    }

    @PostMapping("/jump-question/{questionId}")
    public ApiResponse<QuestionResponse> jumpToQuestion(@RequestHeader("Authorization") String token,
                                                        @PathVariable Integer questionId) {
        Integer userId = getUserIdFromToken(token);

        contestService.jumpToQuestion(userId, questionId);
        Question question = contestService.getCurrentQuestion(userId);

        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }

        return ApiResponse.success(QuestionResponse.from(question));
    }

    @GetMapping("/rankings")
    public ApiResponse<List<RankingItem>> getRankings() {
        List<RankingItem> rankings = contestService.getRankings();
        return ApiResponse.success(rankings);
    }

    @PostMapping("/finalize")
    public ApiResponse<String> finalizeSubmission(@RequestHeader("Authorization") String token) {
        Integer userId = getUserIdFromToken(token);

        contestService.finalizeSubmission(userId);
        log.info("Submission finalized for user: userId={}", userId);
        return ApiResponse.success(null, "Submission finalized");
    }

    @PostMapping("/auto-finalize-all")
    public ApiResponse<String> autoFinalizeAll() {
        log.info("Auto-finalizing all submissions");
        return ApiResponse.success(null, "All submissions finalized");
    }

    @GetMapping("/hints/{questionId}")
    public ApiResponse<List<HintDTO>> getHintsByQuestion(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer questionId) {
        Integer userId = getUserIdFromToken(token);

        List<HintDTO> hints = hintService.getHintsWithUnlockStatus(questionId, userId);
        return ApiResponse.success(hints);
    }

    @PostMapping("/unlock-hint/{hintId}")
    public ApiResponse<HintDTO> unlockHint(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer hintId) {
        Integer userId = getUserIdFromToken(token);

        if (!contestTimeUtil.isContestActive()) {
            throw new IllegalArgumentException("Contest is not active");
        }

        HintDTO hint = hintService.unlockHint(userId, hintId);
        log.info("Hint unlocked: userId={}, hintId={}", userId, hintId);
        return ApiResponse.success(hint, "Hint unlocked successfully");
    }

    @GetMapping("/announcements/latest")
    public ApiResponse<Announcement> getLatestActiveAnnouncement() {
        Announcement announcement = announcementService.getLatestActiveAnnouncement();
        return ApiResponse.success(announcement);
    }
}
