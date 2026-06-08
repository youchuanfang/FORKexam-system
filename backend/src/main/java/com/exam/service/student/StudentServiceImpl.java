package com.exam.service.student;

import com.exam.common.UserContext;
import com.exam.dto.student.AnswerDetailDTO;
import com.exam.dto.student.ExamRecordDTO;
import com.exam.dto.student.ExamRecordDetailDTO;
import com.exam.dto.student.PaperDetailDTO;
import com.exam.dto.student.PaperListDTO;
import com.exam.dto.student.QuestionDTO;
import com.exam.dto.student.StartExamRequest;
import com.exam.dto.student.StartExamResponse;
import com.exam.dto.student.SubmitAnswerDTO;
import com.exam.dto.student.SubmitExamRequest;
import com.exam.dto.student.SubmitExamResponse;
import com.exam.entity.AnswerDetail;
import com.exam.entity.ExamRecord;
import com.exam.entity.Paper;
import com.exam.entity.PaperQuestion;
import com.exam.entity.Question;
import com.exam.repository.AnswerDetailRepository;
import com.exam.repository.ExamRecordRepository;
import com.exam.repository.PaperQuestionRepository;
import com.exam.repository.PaperRepository;
import com.exam.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    private static final String ROLE_STUDENT = "student";
    private static final Set<String> AUTO_GRADE_TYPES = Set.of(
            "single_choice", "multi_choice", "true_false", "fill_blank"
    );

    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ExamRecordRepository examRecordRepository;
    private final AnswerDetailRepository answerDetailRepository;

    public StudentServiceImpl(PaperRepository paperRepository,
                              PaperQuestionRepository paperQuestionRepository,
                              QuestionRepository questionRepository,
                              ExamRecordRepository examRecordRepository,
                              AnswerDetailRepository answerDetailRepository) {
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.examRecordRepository = examRecordRepository;
        this.answerDetailRepository = answerDetailRepository;
    }

    @Override
    public List<PaperListDTO> getPapers() {
        Integer studentId = ensureStudent();
        List<ExamRecord> studentRecords = examRecordRepository.findByStudentId(studentId);
        return paperRepository.findAll().stream()
                .sorted(Comparator.comparing(Paper::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(paper -> buildPaperListDTO(paper, studentRecords))
                .collect(Collectors.toList());
    }

    @Override
    public PaperDetailDTO getPaperDetail(Integer paperId) {
        Integer studentId = ensureStudent();
        Paper paper = getPaperOrThrow(paperId);
        List<ExamRecord> studentRecords = examRecordRepository.findByStudentId(studentId);
        return buildPaperBasicDetail(paper, studentRecords);
    }

    @Override
    @Transactional
    public StartExamResponse startExam(StartExamRequest request) {
        Integer studentId = ensureStudent();
        if (request == null || request.getPaperId() == null) {
            throw new RuntimeException("paperId涓嶈兘涓虹┖");
        }

        Paper paper = getPaperOrThrow(request.getPaperId());
        List<ExamRecord> studentRecords = examRecordRepository.findByStudentId(studentId);
        PaperListDTO availability = buildPaperListDTO(paper, studentRecords);
        if (!"OPEN".equals(availability.getStatus())) {
            throw new RuntimeException(availability.getStatusText());
        }

        ExamRecord record = new ExamRecord();
        record.setStudentId(studentId);
        record.setPaperId(paper.getId());
        record.setStartTime(LocalDateTime.now());
        record.setTotalScore(0.0);
        record = examRecordRepository.save(record);

        StartExamResponse response = new StartExamResponse();
        response.setRecordId(record.getId());
        response.setPaperId(paper.getId());
        response.setTitle(paper.getTitle());
        response.setStartTime(record.getStartTime());
        response.setOpenStartTime(paper.getOpenStartTime());
        response.setOpenEndTime(paper.getOpenEndTime());
        response.setDuration(paper.getDuration());
        response.setQuestions(buildQuestions(paper.getId()));
        return response;
    }

    @Override
    @Transactional
    public SubmitExamResponse submitExam(Integer recordId, SubmitExamRequest request) {
        Integer studentId = ensureStudent();
        ExamRecord record = getRecordOrThrow(recordId);
        ensureRecordOwner(record, studentId);
        if (record.getSubmitTime() != null) {
            throw new RuntimeException("璇ヨ€冭瘯璁板綍宸叉彁浜わ紝涓嶈兘閲嶅鎻愪氦");
        }
        Paper paper = getPaperOrThrow(record.getPaperId());
        ensurePaperOpenForAnswering(paper);

        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByPaperId(record.getPaperId());
        if (paperQuestions.isEmpty()) {
            throw new RuntimeException("璇曞嵎娌℃湁棰樼洰锛屼笉鑳芥彁浜");
        }

        Map<Integer, PaperQuestion> paperQuestionMap = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, Function.identity(), (left, right) -> left));
        validateSubmittedQuestionIds(request, paperQuestionMap.keySet());
        Map<Integer, Question> questionMap = questionRepository.findAllById(paperQuestionMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Integer, String> submittedAnswers = toAnswerMap(request);

        List<AnswerDetail> details = new ArrayList<>();
        double totalScore = 0.0;
        double objectiveScore = 0.0;

        for (PaperQuestion paperQuestion : paperQuestions) {
            Question question = questionMap.get(paperQuestion.getQuestionId());
            if (question == null) {
                continue;
            }

            String studentAnswer = submittedAnswers.getOrDefault(question.getId(), "");
            double questionScore = paperQuestion.getScore() == null ? 0.0 : paperQuestion.getScore();
            GradeResult grade = gradeAnswer(question, studentAnswer, questionScore);

            AnswerDetail detail = new AnswerDetail();
            detail.setRecordId(record.getId());
            detail.setQuestionId(question.getId());
            detail.setStudentAnswer(studentAnswer);
            detail.setIsCorrect(grade.isCorrect());
            detail.setScoreGot(grade.scoreGot());
            details.add(detail);

            totalScore += grade.scoreGot();
            if (AUTO_GRADE_TYPES.contains(question.getType())) {
                objectiveScore += grade.scoreGot();
            }
        }

        answerDetailRepository.saveAll(details);
        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(totalScore);
        examRecordRepository.save(record);

        SubmitExamResponse response = new SubmitExamResponse();
        response.setRecordId(record.getId());
        response.setTotalScore(totalScore);
        response.setObjectiveScore(objectiveScore);
        response.setMessage("鎻愪氦鎴愬姛锛屼富瑙傞鏆傛寜0鍒嗚鍏ワ紝寰呮暀甯堟壒鏀");
        return response;
    }

    @Override
    public List<ExamRecordDTO> getMyExamRecords() {
        Integer studentId = ensureStudent();
        Map<Integer, Paper> paperMap = paperRepository.findAll().stream()
                .collect(Collectors.toMap(Paper::getId, Function.identity()));

        return examRecordRepository.findByStudentId(studentId).stream()
                .sorted(Comparator.comparing(ExamRecord::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(record -> toExamRecordDTO(record, paperMap.get(record.getPaperId())))
                .collect(Collectors.toList());
    }

    @Override
    public ExamRecordDetailDTO getExamRecordDetail(Integer recordId) {
        Integer studentId = ensureStudent();
        ExamRecord record = getRecordOrThrow(recordId);
        ensureRecordOwner(record, studentId);
        Paper paper = getPaperOrThrow(record.getPaperId());

        Map<Integer, PaperQuestion> scoreMap = paperQuestionRepository.findByPaperId(record.getPaperId()).stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, Function.identity(), (left, right) -> left));
        Map<Integer, Question> questionMap = questionRepository.findAllById(scoreMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        boolean teacherOpenAnswer = Boolean.TRUE.equals(paper.getTeacherOpenAnswer());
        boolean canReturnAnswers = teacherOpenAnswer && isAnswerAvailable(paper, record);
        List<AnswerDetailDTO> answers = answerDetailRepository.findByRecordId(record.getId()).stream()
                .sorted(Comparator.comparing(AnswerDetail::getQuestionId, Comparator.nullsLast(Integer::compareTo)))
                .map(detail -> toAnswerDetailDTO(
                        detail,
                        questionMap.get(detail.getQuestionId()),
                        scoreMap.get(detail.getQuestionId()),
                        canReturnAnswers
                ))
                .collect(Collectors.toList());

        ExamRecordDetailDTO dto = new ExamRecordDetailDTO();
        dto.setRecordId(record.getId());
        dto.setPaperId(record.getPaperId());
        dto.setPaperTitle(paper.getTitle());
        dto.setDuration(paper.getDuration());
        dto.setOpenStartTime(paper.getOpenStartTime());
        dto.setOpenEndTime(paper.getOpenEndTime());
        dto.setStartTime(record.getStartTime());
        dto.setSubmitTime(record.getSubmitTime());
        dto.setTotalScore(record.getTotalScore());
        dto.setTeacherOpenAnswer(teacherOpenAnswer);
        dto.setReferenceAnswerMap(canReturnAnswers ? buildReferenceAnswerMap(questionMap) : new HashMap<>());
        if (record.getSubmitTime() == null) {
            dto.setQuestions(buildQuestions(record.getPaperId()));
            dto.setAnswers(new ArrayList<>());
        } else {
            dto.setQuestions(new ArrayList<>());
            dto.setAnswers(answers);
        }
        return dto;
    }

    private PaperDetailDTO buildPaperBasicDetail(Paper paper, List<ExamRecord> studentRecords) {
        PaperListDTO summary = buildPaperListDTO(paper, studentRecords);
        PaperDetailDTO dto = new PaperDetailDTO();
        dto.setPaperId(summary.getPaperId());
        dto.setTitle(summary.getTitle());
        dto.setDuration(summary.getDuration());
        dto.setOpenStartTime(summary.getOpenStartTime());
        dto.setOpenEndTime(summary.getOpenEndTime());
        dto.setMaxAttempts(summary.getMaxAttempts());
        dto.setAttemptCount(summary.getAttemptCount());
        dto.setRemainingAttempts(summary.getRemainingAttempts());
        dto.setBestScore(summary.getBestScore());
        dto.setStatus(summary.getStatus());
        dto.setStatusText(summary.getStatusText());
        dto.setTeacherOpenAnswer(Boolean.TRUE.equals(paper.getTeacherOpenAnswer()));
        dto.setQuestions(new ArrayList<>());
        return dto;
    }

    private List<QuestionDTO> buildQuestions(Integer paperId) {
        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByPaperId(paperId);
        Map<Integer, Question> questionMap = questionRepository.findAllById(
                paperQuestions.stream().map(PaperQuestion::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, Function.identity()));

        List<QuestionDTO> questions = paperQuestions.stream()
                .map(paperQuestion -> {
                    Question question = questionMap.get(paperQuestion.getQuestionId());
                    if (question == null) {
                        return null;
                    }
                    return new QuestionDTO(
                            question.getId(),
                            question.getType(),
                            question.getContent(),
                            question.getOptions(),
                            paperQuestion.getScore()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return questions;
    }

    private PaperListDTO buildPaperListDTO(Paper paper, List<ExamRecord> studentRecords) {
        int maxAttempts = paper.getMaxAttempts() != null ? paper.getMaxAttempts() : 1;
        List<ExamRecord> recordsForPaper = studentRecords.stream()
                .filter(record -> Objects.equals(record.getPaperId(), paper.getId()))
                .collect(Collectors.toList());
        int attemptCount = recordsForPaper.size();
        int remainingAttempts = Math.max(0, maxAttempts - attemptCount);
        Double bestScore = recordsForPaper.stream()
                .map(ExamRecord::getTotalScore)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);

        PaperListDTO dto = new PaperListDTO(paper.getId(), paper.getTitle(), paper.getDuration());
        dto.setOpenStartTime(formatDateTime(paper.getOpenStartTime()));
        dto.setOpenEndTime(formatDateTime(paper.getOpenEndTime()));
        dto.setMaxAttempts(maxAttempts);
        dto.setAttemptCount(attemptCount);
        dto.setRemainingAttempts(remainingAttempts);
        dto.setBestScore(bestScore);
        LocalDateTime now = LocalDateTime.now();
        if (paper.getOpenStartTime() != null && now.isBefore(paper.getOpenStartTime())) {
            dto.setStatus("NOT_OPEN");
            dto.setStatusText("鑰冭瘯鏈紑鏀");
        } else if (paper.getOpenEndTime() != null && !now.isBefore(paper.getOpenEndTime())) {
            dto.setStatus("CLOSED");
            dto.setStatusText("鑰冭瘯宸叉埅姝");
        } else if (remainingAttempts <= 0) {
            dto.setStatus("NO_ATTEMPTS");
            dto.setStatusText("浣滅瓟娆℃暟宸茬敤瀹");
        } else {
            dto.setStatus("OPEN");
            dto.setStatusText("宸插紑鏀");
        }
        return dto;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private void validateSubmittedQuestionIds(SubmitExamRequest request, Set<Integer> paperQuestionIds) {
        if (request == null || request.getAnswers() == null) {
            return;
        }
        for (SubmitAnswerDTO answer : request.getAnswers()) {
            if (answer == null || answer.getQuestionId() == null) {
                throw new RuntimeException("鎻愪氦绛旀涓瓨鍦ㄧ己灏憅uestionId鐨勯」鐩");
            }
            if (!paperQuestionIds.contains(answer.getQuestionId())) {
                throw new RuntimeException("鎻愪氦绛旀涓寘鍚笉灞炰簬褰撳墠璇曞嵎鐨勯鐩");
            }
        }
    }

    private Map<Integer, String> toAnswerMap(SubmitExamRequest request) {
        Map<Integer, String> answerMap = new HashMap<>();
        if (request == null || request.getAnswers() == null) {
            return answerMap;
        }
        for (SubmitAnswerDTO answer : request.getAnswers()) {
            if (answer != null && answer.getQuestionId() != null) {
                answerMap.put(answer.getQuestionId(), normalizeAnswerValue(answer.getAnswer()));
            }
        }
        return answerMap;
    }

    private GradeResult gradeAnswer(Question question, String studentAnswer, double score) {
        String type = question.getType();
        if ("short_answer".equals(type)) {
            return new GradeResult(false, 0.0);
        }

        boolean correct;
        if ("multi_choice".equals(type)) {
            correct = normalizeMultiChoice(studentAnswer).equals(normalizeMultiChoice(question.getAnswer()));
        } else if ("fill_blank".equals(type)) {
            correct = normalizeText(studentAnswer).equals(normalizeText(question.getAnswer()));
        } else if ("single_choice".equals(type) || "true_false".equals(type)) {
            correct = normalizeText(studentAnswer).equals(normalizeText(question.getAnswer()));
        } else {
            correct = false;
        }

        return new GradeResult(correct, correct ? score : 0.0);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeAnswerValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> item == null ? "" : String.valueOf(item).trim())
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.joining(","));
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<String> items = new ArrayList<>();
            for (Object item : array) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    items.add(String.valueOf(item).trim());
                }
            }
            return String.join(",", items);
        }
        return normalizeText(String.valueOf(value));
    }

    private String normalizeMultiChoice(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String text = value.trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1)
                    .replace("\"", "")
                    .replace("'", "");
        }
        String[] parts = text.split("[,锛宂");
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            String item = part.trim();
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        normalized.sort(String::compareTo);
        return String.join(",", normalized);
    }

    private Integer ensureStudent() {
        Integer userId = UserContext.getUserId();
        String role = UserContext.getRole();
        if (userId == null) {
            throw new RuntimeException("鏈櫥褰曟垨token鏃犳晥");
        }
        if (!ROLE_STUDENT.equals(role)) {
            throw new RuntimeException("浠呭鐢熻鑹插彲浠ヨ闂鎺ュ彛");
        }
        return userId;
    }

    private Paper getPaperOrThrow(Integer paperId) {
        if (paperId == null) {
            throw new RuntimeException("paperId涓嶈兘涓虹┖");
        }
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("璇曞嵎涓嶅瓨鍦?"));
    }

    private ExamRecord getRecordOrThrow(Integer recordId) {
        if (recordId == null) {
            throw new RuntimeException("recordId涓嶈兘涓虹┖");
        }
        return examRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("鑰冭瘯璁板綍涓嶅瓨鍦?"));
    }

    private void ensureRecordOwner(ExamRecord record, Integer studentId) {
        if (!Objects.equals(record.getStudentId(), studentId)) {
            throw new RuntimeException("涓嶈兘璁块棶鎴栨彁浜ゅ埆浜虹殑鑰冭瘯璁板綍");
        }
    }

    private void ensurePaperOpenForAnswering(Paper paper) {
        LocalDateTime now = LocalDateTime.now();
        if (paper.getOpenStartTime() != null && now.isBefore(paper.getOpenStartTime())) {
            throw new RuntimeException("鑰冭瘯灏氭湭鍒板紑鏀炬椂闂达紝涓嶈兘浣滅瓟");
        }
        if (paper.getOpenEndTime() != null && !now.isBefore(paper.getOpenEndTime())) {
            throw new RuntimeException("鑰冭瘯宸叉埅姝紝涓嶈兘浣滅瓟");
        }
    }

    private ExamRecordDTO toExamRecordDTO(ExamRecord record, Paper paper) {
        ExamRecordDTO dto = new ExamRecordDTO();
        dto.setRecordId(record.getId());
        dto.setPaperId(record.getPaperId());
        dto.setPaperTitle(paper == null ? "鏈煡璇曞嵎" : paper.getTitle());
        dto.setStartTime(record.getStartTime());
        dto.setSubmitTime(record.getSubmitTime());
        dto.setTotalScore(record.getTotalScore());
        return dto;
    }

    private AnswerDetailDTO toAnswerDetailDTO(AnswerDetail detail,
                                              Question question,
                                              PaperQuestion paperQuestion,
                                              boolean canReturnAnswers) {
        AnswerDetailDTO dto = new AnswerDetailDTO();
        dto.setQuestionId(detail.getQuestionId());
        dto.setStudentAnswer(detail.getStudentAnswer());
        dto.setIsCorrect(detail.getIsCorrect());
        dto.setScoreGot(detail.getScoreGot());
        if (question != null) {
            dto.setType(question.getType());
            dto.setContent(question.getContent());
            dto.setOptions(question.getOptions());
            if (canReturnAnswers && AUTO_GRADE_TYPES.contains(question.getType())) {
                dto.setCorrectAnswer(question.getAnswer());
            }
        }
        if (paperQuestion != null) {
            dto.setScore(paperQuestion.getScore());
        }
        return dto;
    }

    private boolean isAnswerAvailable(Paper paper, ExamRecord record) {
        if (Boolean.TRUE.equals(paper.getReleaseAnswerFlag())) {
            if (paper.getAnswerReleaseTime() != null) {
                return !LocalDateTime.now().isBefore(paper.getAnswerReleaseTime());
            }
            return true;
        }
        return isAnswerReviewTimeReached(record, paper);
    }

    private boolean isAnswerReviewTimeReached(ExamRecord record, Paper paper) {
        if (paper.getOpenEndTime() != null) {
            return !LocalDateTime.now().isBefore(paper.getOpenEndTime());
        }
        if (record.getSubmitTime() != null) {
            return true;
        }
        if (record.getStartTime() == null || paper.getDuration() == null) {
            return false;
        }
        return !LocalDateTime.now().isBefore(record.getStartTime().plusMinutes(paper.getDuration()));
    }

    private Map<Integer, String> buildReferenceAnswerMap(Map<Integer, Question> questionMap) {
        Map<Integer, String> referenceAnswerMap = new HashMap<>();
        for (Question question : questionMap.values()) {
            if (question == null || !"short_answer".equals(question.getType())) {
                continue;
            }
            String referenceAnswer = firstNonBlank(question.getReferenceAnswer(), question.getAnswer());
            if (referenceAnswer != null) {
                referenceAnswerMap.put(question.getId(), referenceAnswer);
            }
        }
        return referenceAnswerMap;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }

    private record GradeResult(Boolean isCorrect, Double scoreGot) {}
}


