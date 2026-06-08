package com.exam.service.teacher;

import com.exam.common.UserContext;
import com.exam.dto.teacher.*;
import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherServiceImpl implements TeacherService {

    private static final String ROLE_TEACHER = "teacher";
    private static final Set<String> AUTO_GRADE_TYPES = Set.of(
            "single_choice", "multi_choice", "true_false", "fill_blank"
    );

    private final QuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final ExamRecordRepository examRecordRepository;
    private final AnswerDetailRepository answerDetailRepository;
    private final UserRepository userRepository;

    public TeacherServiceImpl(QuestionRepository questionRepository,
                              PaperRepository paperRepository,
                              PaperQuestionRepository paperQuestionRepository,
                              ExamRecordRepository examRecordRepository,
                              AnswerDetailRepository answerDetailRepository,
                              UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.examRecordRepository = examRecordRepository;
        this.answerDetailRepository = answerDetailRepository;
        this.userRepository = userRepository;
    }

    // ==================== Question Management ====================

    @Override
    public List<QuestionDTO> getQuestions(String type) {
        Integer teacherId = ensureTeacher();
        List<Question> questions;
        if (type != null && !type.isBlank()) {
            questions = questionRepository.findAccessibleByType(teacherId, type);
        } else {
            questions = questionRepository.findAccessibleQuestions(teacherId);
        }
        return questions.stream().map(this::toQuestionDTO).collect(Collectors.toList());
    }

    @Override
    public QuestionDTO createQuestion(QuestionDTO dto) {
        Integer teacherId = ensureTeacher();
        if (dto.getType() == null || dto.getType().isBlank()) {
            throw new RuntimeException("题目类型不能为空");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new RuntimeException("题目内容不能为空");
        }
        Question question = new Question();
        question.setType(dto.getType());
        question.setContent(dto.getContent());
        question.setOptions(dto.getOptions());
        question.setAnswer(dto.getAnswer());
        question.setReferenceAnswer(dto.getReferenceAnswer());
        question.setCourseId(dto.getCourseId());
        question.setCreatedBy(teacherId);
        question = questionRepository.save(question);
        return toQuestionDTO(question);
    }

    @Override
    public QuestionDTO updateQuestion(Integer id, QuestionDTO dto) {
        Integer teacherId = ensureTeacher();
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        if (question.getCreatedBy() != null && !Objects.equals(question.getCreatedBy(), teacherId)) {
            throw new RuntimeException("只能修改自己创建的题目");
        }
        if (dto.getType() != null) question.setType(dto.getType());
        if (dto.getContent() != null) question.setContent(dto.getContent());
        if (dto.getOptions() != null) question.setOptions(dto.getOptions());
        if (dto.getAnswer() != null) question.setAnswer(dto.getAnswer());
        if (dto.getReferenceAnswer() != null) question.setReferenceAnswer(dto.getReferenceAnswer());
        if (dto.getCourseId() != null) question.setCourseId(dto.getCourseId());
        question = questionRepository.save(question);
        return toQuestionDTO(question);
    }

    @Override
    public void deleteQuestion(Integer id) {
        Integer teacherId = ensureTeacher();
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        if (question.getCreatedBy() != null && !Objects.equals(question.getCreatedBy(), teacherId)) {
            throw new RuntimeException("只能删除自己创建的题目");
        }
        questionRepository.deleteById(id);
    }

    // ==================== Paper Management ====================

    @Override
    public List<PaperListDTO> getPapers() {
        Integer teacherId = ensureTeacher();
        List<Paper> papers = paperRepository.findByCreatedBy(teacherId);
        Map<Integer, Long> questionCounts = new HashMap<>();
        Map<Integer, Long> recordCounts = new HashMap<>();
        for (Paper paper : papers) {
            List<PaperQuestion> pqs = paperQuestionRepository.findByPaperId(paper.getId());
            questionCounts.put(paper.getId(), (long) pqs.size());
            List<ExamRecord> records = examRecordRepository.findByPaperId(paper.getId());
            recordCounts.put(paper.getId(), (long) records.size());
        }
        return papers.stream()
                .sorted(Comparator.comparing(Paper::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(paper -> toPaperListDTO(paper, questionCounts, recordCounts))
                .collect(Collectors.toList());
    }

    @Override
    public PaperListDTO getPaper(Integer paperId) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师的试卷");
        }
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperId(paperId);
        List<ExamRecord> records = examRecordRepository.findByPaperId(paperId);
        Map<Integer, Long> qc = Map.of(paperId, (long) pqs.size());
        Map<Integer, Long> rc = Map.of(paperId, (long) records.size());
        return toPaperListDTO(paper, qc, rc);
    }

    @Override
    @Transactional
    public PaperListDTO createPaper(PaperListDTO dto) {
        Integer teacherId = ensureTeacher();
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new RuntimeException("试卷标题不能为空");
        }
        Paper paper = new Paper();
        paper.setTitle(dto.getTitle());
        paper.setDuration(dto.getDuration());
        paper.setCreatedBy(teacherId);
        paper.setOpenStartTime(dto.getOpenStartTime());
        paper.setOpenEndTime(dto.getOpenEndTime());
        paper.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 1);
        boolean releaseAnswerFlag = Boolean.TRUE.equals(dto.getReleaseAnswerFlag());
        paper.setReleaseAnswerFlag(releaseAnswerFlag);
        paper.setTeacherOpenAnswer(releaseAnswerFlag);
        paper.setAnswerReleaseTime(releaseAnswerFlag ? dto.getAnswerReleaseTime() : null);
        paper = paperRepository.save(paper);
        return toPaperListDTO(paper, Map.of(), Map.of());
    }

    @Override
    public PaperListDTO updatePaper(Integer paperId, PaperListDTO dto) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能修改其他教师的试卷");
        }
        if (dto.getTitle() != null) paper.setTitle(dto.getTitle());
        if (dto.getDuration() != null) paper.setDuration(dto.getDuration());
        paper.setOpenStartTime(dto.getOpenStartTime());
        paper.setOpenEndTime(dto.getOpenEndTime());
        if (dto.getMaxAttempts() != null) paper.setMaxAttempts(dto.getMaxAttempts());
        if (dto.getReleaseAnswerFlag() != null) {
            paper.setReleaseAnswerFlag(dto.getReleaseAnswerFlag());
            paper.setTeacherOpenAnswer(dto.getReleaseAnswerFlag());
        }
        paper.setAnswerReleaseTime(Boolean.TRUE.equals(paper.getReleaseAnswerFlag()) ? dto.getAnswerReleaseTime() : null);
        paper = paperRepository.save(paper);
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperId(paperId);
        List<ExamRecord> records = examRecordRepository.findByPaperId(paperId);
        Map<Integer, Long> qc = Map.of(paperId, (long) pqs.size());
        Map<Integer, Long> rc = Map.of(paperId, (long) records.size());
        return toPaperListDTO(paper, qc, rc);
    }

    @Override
    @Transactional
    public void deletePaper(Integer paperId) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能删除其他教师的试卷");
        }
        if (!examRecordRepository.findByPaperId(paperId).isEmpty()) {
            throw new RuntimeException("该试卷已有考试记录，不能删除，可后续改为归档/禁用");
        }
        paperQuestionRepository.deleteByPaperId(paperId);
        paperRepository.deleteById(paperId);
    }

    @Override
    public List<PaperQuestionDTO> getPaperQuestions(Integer paperId) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师的试卷");
        }
        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByPaperId(paperId);
        Map<Integer, Question> questionMap = questionRepository.findAllById(
                paperQuestions.stream()
                        .map(PaperQuestion::getQuestionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q));

        return paperQuestions.stream().map(pq -> {
            PaperQuestionDTO dto = new PaperQuestionDTO();
            dto.setQuestionId(pq.getQuestionId());
            dto.setScore(pq.getScore());
            Question question = questionMap.get(pq.getQuestionId());
            if (question != null) {
                dto.setQuestion(toQuestionDTO(question));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignQuestions(Integer paperId, List<PaperQuestionDTO> questions) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能修改其他教师的试卷");
        }
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("题目列表不能为空");
        }
        // Remove old questions
        paperQuestionRepository.deleteByPaperId(paperId);
        // Add new ones
        for (PaperQuestionDTO pq : questions) {
            if (!questionRepository.existsById(pq.getQuestionId())) {
                throw new RuntimeException("题目 " + pq.getQuestionId() + " 不存在");
            }
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paperId);
            paperQuestion.setQuestionId(pq.getQuestionId());
            paperQuestion.setScore(pq.getScore() != null ? pq.getScore() : 0.0);
            paperQuestionRepository.save(paperQuestion);
        }
    }

    // ==================== Exam Records ====================

    @Override
    public List<ExamRecordDTO> getExamRecords(Integer paperId) {
        Integer teacherId = ensureTeacher();
        if (paperId == null) {
            return new ArrayList<>();
        }
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师的考试记录");
        }
        List<ExamRecord> records = examRecordRepository.findByPaperId(paperId);
        Map<Integer, String> userNames = new HashMap<>();
        Map<Integer, Boolean> subjectiveCache = new HashMap<>();
        return records.stream().map(record -> {
            String studentName = userNames.computeIfAbsent(record.getStudentId(), sid ->
                    userRepository.findById(sid).map(User::getUsername).orElse("未知")
            );
            boolean hasSubjective = subjectiveCache.computeIfAbsent(record.getId(), rid -> {
                List<AnswerDetail> details = answerDetailRepository.findByRecordId(rid);
                return details.stream().anyMatch(d -> {
                    Question q = questionRepository.findById(d.getQuestionId()).orElse(null);
                    return q != null && "short_answer".equals(q.getType());
                });
            });
            return toExamRecordDTO(record, paper, studentName, hasSubjective);
        }).collect(Collectors.toList());
    }

    @Override
    public ExamRecordDetailDTO getExamRecordDetail(Integer recordId) {
        Integer teacherId = ensureTeacher();
        ExamRecord record = examRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("考试记录不存在"));
        Paper paper = getPaperOrThrow(record.getPaperId());
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师试卷的考试记录");
        }
        String studentName = userRepository.findById(record.getStudentId())
                .map(User::getUsername).orElse("未知");

        ExamRecordDetailDTO dto = new ExamRecordDetailDTO();
        dto.setRecordId(record.getId());
        dto.setPaperId(record.getPaperId());
        dto.setPaperTitle(paper.getTitle());
        dto.setStudentId(record.getStudentId());
        dto.setStudentName(studentName);
        dto.setStartTime(record.getStartTime());
        dto.setSubmitTime(record.getSubmitTime());
        dto.setTotalScore(record.getTotalScore());
        dto.setSubmitted(record.getSubmitTime() != null);

        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperId(record.getPaperId());
        Map<Integer, Double> scoreMap = pqs.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore, (a, b) -> a));
        Map<Integer, Question> questionMap = questionRepository.findAllById(
                pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<AnswerDetail> details = answerDetailRepository.findByRecordId(recordId);
        List<ExamRecordDetailDTO.AnswerItem> items = new ArrayList<>();
        for (AnswerDetail detail : details) {
            Question question = questionMap.get(detail.getQuestionId());
            ExamRecordDetailDTO.AnswerItem item = new ExamRecordDetailDTO.AnswerItem();
            item.setQuestionId(detail.getQuestionId());
            item.setStudentAnswer(detail.getStudentAnswer());
            item.setIsCorrect(detail.getIsCorrect());
            item.setScoreGot(detail.getScoreGot());
            item.setScore(scoreMap.getOrDefault(detail.getQuestionId(), 0.0));
            if (question != null) {
                item.setType(question.getType());
                item.setContent(question.getContent());
                item.setOptions(question.getOptions());
                item.setCorrectAnswer(question.getAnswer());
                item.setAutoGraded(AUTO_GRADE_TYPES.contains(question.getType()));
            }
            items.add(item);
        }
        dto.setAnswers(items);
        return dto;
    }

    // ==================== Grading ====================

    @Override
    @Transactional
    public void gradeQuestion(Integer recordId, GradeRequest request) {
        Integer teacherId = ensureTeacher();
        if (request.getQuestionId() == null || request.getScoreGot() == null) {
            throw new RuntimeException("questionId和scoreGot不能为空");
        }
        if (request.getScoreGot() < 0) {
            throw new RuntimeException("得分不能为负数");
        }

        ExamRecord record = examRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("考试记录不存在"));
        Paper paper = getPaperOrThrow(record.getPaperId());
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能批改其他教师试卷的答案");
        }

        List<AnswerDetail> details = answerDetailRepository.findByRecordIdAndQuestionId(recordId, request.getQuestionId());
        if (details.isEmpty()) {
            throw new RuntimeException("该题目答题记录不存在");
        }
        AnswerDetail detail = details.get(0);

        Question question = questionRepository.findById(request.getQuestionId()).orElse(null);
        if (question == null || !"short_answer".equals(question.getType())) {
            throw new RuntimeException("只能批改简答题");
        }

        Double maxScore = paperQuestionRepository.findByPaperId(record.getPaperId()).stream()
                .filter(pq -> pq.getQuestionId().equals(request.getQuestionId()))
                .map(PaperQuestion::getScore)
                .findFirst().orElse(0.0);

        if (request.getScoreGot() > maxScore) {
            throw new RuntimeException("得分不能超过该题满分 " + maxScore);
        }

        detail.setScoreGot(request.getScoreGot());
        detail.setIsCorrect(request.getScoreGot() >= maxScore);
        answerDetailRepository.save(detail);

        // Recalculate total
        recalculateTotalScore(recordId);
    }

    // ==================== Helper Methods ====================

    private Integer ensureTeacher() {
        Integer userId = UserContext.getUserId();
        String role = UserContext.getRole();
        if (userId == null) {
            throw new RuntimeException("未登录或token无效");
        }
        if (!ROLE_TEACHER.equals(role)) {
            throw new RuntimeException("仅教师角色可以访问该接口");
        }
        return userId;
    }

    private Paper getPaperOrThrow(Integer paperId) {
        if (paperId == null) {
            throw new RuntimeException("paperId不能为空");
        }
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));
    }

    private void recalculateTotalScore(Integer recordId) {
        List<AnswerDetail> details = answerDetailRepository.findByRecordId(recordId);
        double total = details.stream()
                .filter(d -> d.getScoreGot() != null)
                .mapToDouble(AnswerDetail::getScoreGot)
                .sum();
        ExamRecord record = examRecordRepository.findById(recordId).orElse(null);
        if (record != null) {
            record.setTotalScore(total);
            examRecordRepository.save(record);
        }
    }

    private QuestionDTO toQuestionDTO(Question q) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(q.getId());
        dto.setType(q.getType());
        dto.setContent(q.getContent());
        dto.setOptions(q.getOptions());
        dto.setAnswer(q.getAnswer());
        dto.setReferenceAnswer(q.getReferenceAnswer());
        dto.setCourseId(q.getCourseId());
        dto.setCreatedBy(q.getCreatedBy());
        return dto;
    }

    private PaperListDTO toPaperListDTO(Paper paper, Map<Integer, Long> questionCounts, Map<Integer, Long> recordCounts) {
        PaperListDTO dto = new PaperListDTO();
        dto.setId(paper.getId());
        dto.setTitle(paper.getTitle());
        dto.setDuration(paper.getDuration());
        dto.setOpenStartTime(paper.getOpenStartTime());
        dto.setOpenEndTime(paper.getOpenEndTime());
        dto.setMaxAttempts(paper.getMaxAttempts());
        dto.setReleaseAnswerFlag(paper.getReleaseAnswerFlag());
        dto.setAnswerReleaseTime(paper.getAnswerReleaseTime());
        dto.setQuestionCount(questionCounts.getOrDefault(paper.getId(), 0L).intValue());
        dto.setRecordCount(recordCounts.getOrDefault(paper.getId(), 0L).intValue());
        return dto;
    }

    private ExamRecordDTO toExamRecordDTO(ExamRecord record, Paper paper, String studentName, boolean hasSubjective) {
        ExamRecordDTO dto = new ExamRecordDTO();
        dto.setRecordId(record.getId());
        dto.setPaperId(record.getPaperId());
        dto.setPaperTitle(paper.getTitle());
        dto.setStudentId(record.getStudentId());
        dto.setStudentName(studentName);
        dto.setStartTime(record.getStartTime());
        dto.setSubmitTime(record.getSubmitTime());
        dto.setTotalScore(record.getTotalScore());
        dto.setSubmitted(record.getSubmitTime() != null);
        dto.setHasSubjective(hasSubjective);
        return dto;
    }
}
