package com.exam.service.teacher;

import com.exam.common.PageResult;
import com.exam.common.UserContext;
import com.exam.dto.common.ClassRoomDTO;
import com.exam.dto.common.LeaderboardItemDTO;
import com.exam.dto.common.PaperTargetDTO;
import com.exam.dto.common.StudentDTO;
import com.exam.dto.teacher.*;
import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ClassRoomRepository classRoomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final PaperTargetRepository paperTargetRepository;

    public TeacherServiceImpl(QuestionRepository questionRepository,
                              PaperRepository paperRepository,
                              PaperQuestionRepository paperQuestionRepository,
                              ExamRecordRepository examRecordRepository,
                              AnswerDetailRepository answerDetailRepository,
                              UserRepository userRepository,
                              ClassRoomRepository classRoomRepository,
                              ClassMemberRepository classMemberRepository,
                              PaperTargetRepository paperTargetRepository) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.examRecordRepository = examRecordRepository;
        this.answerDetailRepository = answerDetailRepository;
        this.userRepository = userRepository;
        this.classRoomRepository = classRoomRepository;
        this.classMemberRepository = classMemberRepository;
        this.paperTargetRepository = paperTargetRepository;
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
    public PageResult<QuestionDTO> getQuestionsPaged(String type, String keyword, int page, int size) {
        Integer teacherId = ensureTeacher();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Question> questionPage;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasType = type != null && !type.isBlank();
        if (hasKeyword) {
            questionPage = questionRepository.searchAccessibleQuestionsPaged(teacherId, keyword.trim(), pageable);
        } else if (hasType) {
            questionPage = questionRepository.findAccessibleByTypePaged(teacherId, type, pageable);
        } else {
            questionPage = questionRepository.findAccessibleQuestionsPaged(teacherId, pageable);
        }
        List<QuestionDTO> content = questionPage.getContent().stream()
                .map(this::toQuestionDTO).collect(Collectors.toList());
        return new PageResult<>(content, questionPage.getTotalElements(),
                questionPage.getTotalPages(), questionPage.getNumber(), questionPage.getSize());
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

    @Override
    @Transactional
    public List<QuestionDTO> createQuestionsBatch(List<QuestionDTO> dtos) {
        Integer teacherId = ensureTeacher();
        List<QuestionDTO> results = new ArrayList<>();
        for (QuestionDTO dto : dtos) {
            if (dto.getType() == null || dto.getType().isBlank()) continue;
            if (dto.getContent() == null || dto.getContent().isBlank()) continue;
            Question question = new Question();
            question.setType(dto.getType());
            question.setContent(dto.getContent());
            question.setOptions(dto.getOptions());
            question.setAnswer(dto.getAnswer());
            question.setReferenceAnswer(dto.getReferenceAnswer());
            question.setCourseId(dto.getCourseId());
            question.setCreatedBy(teacherId);
            question = questionRepository.save(question);
            results.add(toQuestionDTO(question));
        }
        return results;
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
    public PageResult<PaperListDTO> getPapersPaged(int page, int size) {
        Integer teacherId = ensureTeacher();
        List<Paper> allPapers = paperRepository.findByCreatedBy(teacherId);
        // 批量查询优化：一次性获取所有 question 和 record 数量
        Set<Integer> paperIds = allPapers.stream().map(Paper::getId).collect(Collectors.toSet());
        Map<Integer, Long> questionCounts = new HashMap<>();
        Map<Integer, Long> recordCounts = new HashMap<>();
        if (!paperIds.isEmpty()) {
            for (Integer pid : paperIds) {
                questionCounts.put(pid, (long) paperQuestionRepository.findByPaperId(pid).size());
                recordCounts.put(pid, (long) examRecordRepository.findByPaperId(pid).size());
            }
        }
        // 排序后手动分页
        allPapers.sort(Comparator.comparing(Paper::getId, Comparator.nullsLast(Integer::compareTo)).reversed());
        int total = allPapers.size();
        int fromIdx = page * size;
        int toIdx = Math.min(fromIdx + size, total);
        List<PaperListDTO> content;
        if (fromIdx >= total) {
            content = List.of();
        } else {
            content = allPapers.subList(fromIdx, toIdx).stream()
                    .map(p -> toPaperListDTO(p, questionCounts, recordCounts))
                    .collect(Collectors.toList());
        }
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, total, totalPages, page, size);
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
        paper.setPublished(false);
        paper.setLeaderboardPublic(Boolean.TRUE.equals(dto.getLeaderboardPublic()));
        paper = paperRepository.save(paper);
        savePaperTargets(paper.getId(), dto.getTargets(), false);
        return toPaperListDTO(paper, Map.of(), Map.of());
    }

    @Override
    @Transactional
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
        if (dto.getLeaderboardPublic() != null) paper.setLeaderboardPublic(dto.getLeaderboardPublic());
        paper = paperRepository.save(paper);
        if (dto.getTargets() != null) {
            savePaperTargets(paperId, dto.getTargets(), false);
        }
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
        paperTargetRepository.deleteByPaperId(paperId);
        paperRepository.deleteById(paperId);
    }

    @Override
    @Transactional
    public PaperListDTO publishPaper(Integer paperId, List<PaperTargetDTO> targets) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能发布其他教师的试卷");
        }
        if (paper.getTitle() == null || paper.getTitle().isBlank()) {
            throw new RuntimeException("试卷标题不能为空");
        }
        List<PaperQuestion> questions = paperQuestionRepository.findByPaperId(paperId);
        if (questions.isEmpty()) {
            throw new RuntimeException("试卷必须至少包含一道题");
        }
        if (questions.stream().anyMatch(q -> q.getScore() == null || q.getScore() < 0)) {
            throw new RuntimeException("每道题分值必须大于等于0");
        }
        savePaperTargets(paperId, targets, true);
        paper.setPublished(true);
        paper.setPublishedAt(LocalDateTime.now());
        paper = paperRepository.save(paper);
        return toPaperListDTO(paper, Map.of(paperId, (long) questions.size()), Map.of(paperId, (long) examRecordRepository.findByPaperId(paperId).size()));
    }

    @Override
    @Transactional
    public PaperListDTO unpublishPaper(Integer paperId) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能修改其他教师的试卷");
        }
        paper.setPublished(false);
        paper.setPublishedAt(null);
        paper = paperRepository.save(paper);
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperId(paperId);
        return toPaperListDTO(paper, Map.of(paperId, (long) pqs.size()), Map.of(paperId, (long) examRecordRepository.findByPaperId(paperId).size()));
    }

    @Override
    public PaperListDTO updateLeaderboardVisibility(Integer paperId, Boolean visible) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能修改其他教师的试卷");
        }
        paper.setLeaderboardPublic(Boolean.TRUE.equals(visible));
        paper = paperRepository.save(paper);
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperId(paperId);
        List<ExamRecord> records = examRecordRepository.findByPaperId(paperId);
        return toPaperListDTO(paper, Map.of(paperId, (long) pqs.size()), Map.of(paperId, (long) records.size()));
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
        Map<Integer, Double> oldScoreMap = paperQuestionRepository.findByPaperId(paperId).stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq.getScore() == null ? 0.0 : pq.getScore(), (a, b) -> a));
        paperQuestionRepository.deleteByPaperId(paperId);
        Map<Integer, Double> newScoreMap = new HashMap<>();
        for (PaperQuestionDTO pq : questions) {
            if (!questionRepository.existsById(pq.getQuestionId())) {
                throw new RuntimeException("题目 " + pq.getQuestionId() + " 不存在");
            }
            double score = pq.getScore() != null ? pq.getScore() : 0.0;
            if (score < 0) {
                throw new RuntimeException("题目分值必须大于等于0");
            }
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paperId);
            paperQuestion.setQuestionId(pq.getQuestionId());
            paperQuestion.setScore(score);
            paperQuestionRepository.save(paperQuestion);
            newScoreMap.put(pq.getQuestionId(), score);
        }
        recalculateSubmittedRecords(paperId, oldScoreMap, newScoreMap);
    }

    // ==================== Class Management ====================

    @Override
    public List<ClassRoomDTO> getClasses() {
        Integer teacherId = ensureTeacher();
        return classRoomRepository.findByTeacherId(teacherId).stream()
                .map(classRoom -> toClassRoomDTO(classRoom, false))
                .collect(Collectors.toList());
    }

    @Override
    public ClassRoomDTO createClass(String name) {
        Integer teacherId = ensureTeacher();
        if (name == null || name.isBlank()) {
            throw new RuntimeException("班级名称不能为空");
        }
        ClassRoom classRoom = new ClassRoom();
        classRoom.setName(name.trim());
        classRoom.setTeacherId(teacherId);
        classRoom.setJoinCode(generateJoinCode());
        classRoom.setCreatedAt(LocalDateTime.now());
        return toClassRoomDTO(classRoomRepository.save(classRoom), true);
    }

    @Override
    public ClassRoomDTO getClassDetail(Integer classId) {
        Integer teacherId = ensureTeacher();
        ClassRoom classRoom = getOwnedClassOrThrow(classId, teacherId);
        return toClassRoomDTO(classRoom, true);
    }

    @Override
    @Transactional
    public ClassRoomDTO addClassStudent(Integer classId, Integer studentId) {
        Integer teacherId = ensureTeacher();
        ClassRoom classRoom = getOwnedClassOrThrow(classId, teacherId);
        if (studentId == null) {
            throw new RuntimeException("studentId不能为空");
        }
        User student = userRepository.findById(studentId)
                .filter(user -> "student".equals(user.getRole()))
                .orElseThrow(() -> new RuntimeException("指定学生不存在"));
        if (!classMemberRepository.existsByClassIdAndStudentId(classRoom.getId(), student.getId())) {
            ClassMember member = new ClassMember();
            member.setClassId(classRoom.getId());
            member.setStudentId(student.getId());
            member.setJoinedAt(LocalDateTime.now());
            classMemberRepository.save(member);
        }
        return toClassRoomDTO(classRoom, true);
    }

    @Override
    @Transactional
    public void removeClassStudent(Integer classId, Integer studentId) {
        Integer teacherId = ensureTeacher();
        getOwnedClassOrThrow(classId, teacherId);
        classMemberRepository.deleteByClassIdAndStudentId(classId, studentId);
    }

    @Override
    public List<StudentDTO> getStudents(String keyword) {
        ensureTeacher();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return userRepository.findByRole("student").stream()
                .filter(user -> kw.isEmpty() || user.getUsername().toLowerCase().contains(kw))
                .map(user -> new StudentDTO(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
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
    public PageResult<ExamRecordDTO> getExamRecordsPaged(Integer paperId, int page, int size) {
        Integer teacherId = ensureTeacher();
        if (paperId == null) {
            return new PageResult<>(List.of(), 0, 0, page, size);
        }
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师的考试记录");
        }
        List<ExamRecord> allRecords = examRecordRepository.findByPaperId(paperId);
        // 批量查询用户名
        Set<Integer> userIds = allRecords.stream().map(ExamRecord::getStudentId).collect(Collectors.toSet());
        Map<Integer, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        int total = allRecords.size();
        int fromIdx = page * size;
        int toIdx = Math.min(fromIdx + size, total);
        List<ExamRecordDTO> content;
        if (fromIdx >= total) {
            content = List.of();
        } else {
            content = allRecords.subList(fromIdx, toIdx).stream().map(record -> {
                String studentName = userNames.getOrDefault(record.getStudentId(), "未知");
                boolean hasSubjective = answerDetailRepository.findByRecordId(record.getId()).stream().anyMatch(d -> {
                    Question q = questionRepository.findById(d.getQuestionId()).orElse(null);
                    return q != null && "short_answer".equals(q.getType());
                });
                return toExamRecordDTO(record, paper, studentName, hasSubjective);
            }).collect(Collectors.toList());
        }
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, total, totalPages, page, size);
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

    @Override
    public PaperStatisticsDTO getPaperStatistics(Integer paperId, Integer classId, List<Integer> studentIds) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师试卷的统计");
        }
        Map<Integer, ExamRecord> bestRecords = bestSubmittedRecordsByStudent(paperId);
        List<ExamRecord> records = new ArrayList<>(bestRecords.values());
        PaperStatisticsDTO dto = new PaperStatisticsDTO();
        dto.setSubmittedCount(records.size());
        dto.setOverallAverage(average(records));
        dto.setMaxScore(records.stream().map(ExamRecord::getTotalScore).filter(Objects::nonNull).max(Double::compareTo).orElse(null));
        dto.setMinScore(records.stream().map(ExamRecord::getTotalScore).filter(Objects::nonNull).min(Double::compareTo).orElse(null));
        dto.setStudentScores(toStudentScores(records));

        List<ClassRoom> classes = classRoomRepository.findByTeacherId(teacherId);
        for (ClassRoom classRoom : classes) {
            if (classId != null && !Objects.equals(classId, classRoom.getId())) {
                continue;
            }
            Set<Integer> memberIds = classMemberRepository.findByClassId(classRoom.getId()).stream()
                    .map(ClassMember::getStudentId)
                    .collect(Collectors.toSet());
            List<ExamRecord> classRecords = records.stream()
                    .filter(record -> memberIds.contains(record.getStudentId()))
                    .collect(Collectors.toList());
            PaperStatisticsDTO.ClassAverageDTO item = new PaperStatisticsDTO.ClassAverageDTO();
            item.setClassId(classRoom.getId());
            item.setClassName(classRoom.getName());
            item.setSubmittedCount(classRecords.size());
            item.setAverageScore(average(classRecords));
            dto.getClassAverages().add(item);
        }

        if (studentIds != null && !studentIds.isEmpty()) {
            Set<Integer> selected = new HashSet<>(studentIds);
            dto.setSelectedStudentsAverage(average(records.stream()
                    .filter(record -> selected.contains(record.getStudentId()))
                    .collect(Collectors.toList())));
        }

        // 分数分布统计
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-59", 0L);
        distribution.put("60-69", 0L);
        distribution.put("70-79", 0L);
        distribution.put("80-89", 0L);
        distribution.put("90-100", 0L);
        for (ExamRecord record : records) {
            double score = record.getTotalScore() == null ? 0.0 : record.getTotalScore();
            if (score < 60) distribution.merge("0-59", 1L, Long::sum);
            else if (score < 70) distribution.merge("60-69", 1L, Long::sum);
            else if (score < 80) distribution.merge("70-79", 1L, Long::sum);
            else if (score < 90) distribution.merge("80-89", 1L, Long::sum);
            else distribution.merge("90-100", 1L, Long::sum);
        }
        dto.setScoreDistribution(distribution);

        return dto;
    }

    @Override
    public List<LeaderboardItemDTO> getLeaderboard(Integer paperId) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能查看其他教师试卷的排行榜");
        }
        return toLeaderboard(bestSubmittedRecordsByStudent(paperId).values());
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

    // ==================== Export ====================

    @Override
    public void exportPaperRecords(Integer paperId, jakarta.servlet.http.HttpServletResponse response) {
        Integer teacherId = ensureTeacher();
        Paper paper = getPaperOrThrow(paperId);
        if (!Objects.equals(paper.getCreatedBy(), teacherId)) {
            throw new RuntimeException("不能导出其他教师试卷的成绩");
        }

        Map<Integer, ExamRecord> bestRecords = bestSubmittedRecordsByStudent(paperId);
        List<ExamRecord> records = new ArrayList<>(bestRecords.values());
        Map<Integer, String> usernames = userRepository.findAllById(
                records.stream().map(ExamRecord::getStudentId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));

        try {
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("考试成绩");
            org.apache.poi.xssf.usermodel.XSSFRow header = sheet.createRow(0);
            String[] headers = {"序号", "学生姓名", "开始时间", "提交时间", "总分"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (ExamRecord record : records) {
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(usernames.getOrDefault(record.getStudentId(), "未知"));
                row.createCell(2).setCellValue(record.getStartTime() != null ? record.getStartTime().toString() : "");
                row.createCell(3).setCellValue(record.getSubmitTime() != null ? record.getSubmitTime().toString() : "");
                row.createCell(4).setCellValue(record.getTotalScore() != null ? record.getTotalScore() : 0.0);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=paper_" + paperId + "_records.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
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

    private void recalculateSubmittedRecords(Integer paperId, Map<Integer, Double> oldScoreMap, Map<Integer, Double> newScoreMap) {
        List<ExamRecord> records = examRecordRepository.findByPaperIdAndSubmitTimeIsNotNull(paperId);
        if (records.isEmpty()) {
            return;
        }
        Map<Integer, Question> questionMap = questionRepository.findAllById(newScoreMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        for (ExamRecord record : records) {
            List<AnswerDetail> details = answerDetailRepository.findByRecordId(record.getId());
            for (AnswerDetail detail : details) {
                Double newScore = newScoreMap.get(detail.getQuestionId());
                if (newScore == null) {
                    detail.setScoreGot(0.0);
                    answerDetailRepository.save(detail);
                    continue;
                }
                Question question = questionMap.get(detail.getQuestionId());
                if (question == null) {
                    continue;
                }
                if (AUTO_GRADE_TYPES.contains(question.getType())) {
                    detail.setScoreGot(Boolean.TRUE.equals(detail.getIsCorrect()) ? newScore : 0.0);
                } else if ("short_answer".equals(question.getType())) {
                    double oldScore = oldScoreMap.getOrDefault(detail.getQuestionId(), 0.0);
                    double oldGot = detail.getScoreGot() == null ? 0.0 : detail.getScoreGot();
                    if (oldGot <= 0) {
                        detail.setScoreGot(0.0);
                    } else if (oldScore > 0) {
                        detail.setScoreGot(roundScore(oldGot / oldScore * newScore));
                    } else {
                        detail.setScoreGot(Math.min(oldGot, newScore));
                    }
                }
                answerDetailRepository.save(detail);
            }
            recalculateTotalScore(record.getId());
        }
    }

    private double roundScore(double value) {
        return Math.round(value * 100.0) / 100.0;
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
        dto.setPublished(Boolean.TRUE.equals(paper.getPublished()));
        dto.setPublishedAt(paper.getPublishedAt());
        dto.setLeaderboardPublic(Boolean.TRUE.equals(paper.getLeaderboardPublic()));
        dto.setTargets(buildTargetDTOs(paper.getId()));
        dto.setTargetSummary(buildTargetSummary(dto.getTargets()));
        dto.setAverageScore(average(new ArrayList<>(bestSubmittedRecordsByStudent(paper.getId()).values())));
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

    private void savePaperTargets(Integer paperId, List<PaperTargetDTO> targets, boolean requireTargets) {
        if (targets == null || targets.isEmpty()) {
            if (requireTargets) {
                throw new RuntimeException("发布范围必须设置，或明确选择全部学生");
            }
            return;
        }
        Integer teacherId = ensureTeacher();
        paperTargetRepository.deleteByPaperId(paperId);
        boolean hasValidTarget = false;
        for (PaperTargetDTO target : targets) {
            if (target == null || target.getTargetType() == null) {
                continue;
            }
            String type = target.getTargetType().trim().toUpperCase();
            PaperTarget entity = new PaperTarget();
            entity.setPaperId(paperId);
            entity.setTargetType(type);
            if (PaperTarget.TYPE_ALL.equals(type)) {
                entity.setTargetId(null);
                hasValidTarget = true;
            } else if (PaperTarget.TYPE_CLASS.equals(type)) {
                ClassRoom classRoom = getOwnedClassOrThrow(target.getTargetId(), teacherId);
                entity.setTargetId(classRoom.getId());
                hasValidTarget = true;
            } else if (PaperTarget.TYPE_STUDENT.equals(type)) {
                User student = userRepository.findById(target.getTargetId())
                        .filter(user -> "student".equals(user.getRole()))
                        .orElseThrow(() -> new RuntimeException("指定学生不存在"));
                entity.setTargetId(student.getId());
                hasValidTarget = true;
            } else {
                throw new RuntimeException("发布范围类型无效");
            }
            paperTargetRepository.save(entity);
        }
        if (requireTargets && !hasValidTarget) {
            throw new RuntimeException("发布范围必须设置，或明确选择全部学生");
        }
    }

    private List<PaperTargetDTO> buildTargetDTOs(Integer paperId) {
        return paperTargetRepository.findByPaperId(paperId).stream()
                .map(target -> {
                    String name = null;
                    if (PaperTarget.TYPE_ALL.equals(target.getTargetType())) {
                        name = "全部学生";
                    } else if (PaperTarget.TYPE_CLASS.equals(target.getTargetType())) {
                        name = classRoomRepository.findById(target.getTargetId()).map(ClassRoom::getName).orElse("未知班级");
                    } else if (PaperTarget.TYPE_STUDENT.equals(target.getTargetType())) {
                        name = userRepository.findById(target.getTargetId()).map(User::getUsername).orElse("未知学生");
                    }
                    return new PaperTargetDTO(target.getTargetType(), target.getTargetId(), name);
                })
                .collect(Collectors.toList());
    }

    private String buildTargetSummary(List<PaperTargetDTO> targets) {
        if (targets == null || targets.isEmpty()) {
            return "未设置";
        }
        if (targets.stream().anyMatch(target -> PaperTarget.TYPE_ALL.equals(target.getTargetType()))) {
            return "全部学生";
        }
        long classCount = targets.stream().filter(target -> PaperTarget.TYPE_CLASS.equals(target.getTargetType())).count();
        long studentCount = targets.stream().filter(target -> PaperTarget.TYPE_STUDENT.equals(target.getTargetType())).count();
        List<String> parts = new ArrayList<>();
        if (classCount > 0) parts.add(classCount + " 个班级");
        if (studentCount > 0) parts.add(studentCount + " 名学生");
        return String.join("、", parts);
    }

    private ClassRoom getOwnedClassOrThrow(Integer classId, Integer teacherId) {
        if (classId == null) {
            throw new RuntimeException("classId不能为空");
        }
        ClassRoom classRoom = classRoomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("班级不存在"));
        if (!Objects.equals(classRoom.getTeacherId(), teacherId)) {
            throw new RuntimeException("不能管理其他教师的班级");
        }
        return classRoom;
    }

    private ClassRoomDTO toClassRoomDTO(ClassRoom classRoom, boolean includeStudents) {
        ClassRoomDTO dto = new ClassRoomDTO();
        dto.setId(classRoom.getId());
        dto.setName(classRoom.getName());
        dto.setTeacherId(classRoom.getTeacherId());
        dto.setJoinCode(classRoom.getJoinCode());
        dto.setCreatedAt(classRoom.getCreatedAt());
        List<ClassMember> members = classMemberRepository.findByClassId(classRoom.getId());
        dto.setMemberCount(members.size());
        if (includeStudents) {
            Map<Integer, User> users = userRepository.findAllById(
                    members.stream().map(ClassMember::getStudentId).collect(Collectors.toList())
            ).stream().collect(Collectors.toMap(User::getId, user -> user));
            dto.setStudents(members.stream()
                    .map(member -> users.get(member.getStudentId()))
                    .filter(Objects::nonNull)
                    .map(user -> new StudentDTO(user.getId(), user.getUsername()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private String generateJoinCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (classRoomRepository.findByJoinCode(code).isPresent());
        return code;
    }

    private Map<Integer, ExamRecord> bestSubmittedRecordsByStudent(Integer paperId) {
        Map<Integer, ExamRecord> best = new HashMap<>();
        for (ExamRecord record : examRecordRepository.findByPaperIdAndSubmitTimeIsNotNull(paperId)) {
            ExamRecord current = best.get(record.getStudentId());
            if (current == null || compareLeaderboardRecord(record, current) < 0) {
                best.put(record.getStudentId(), record);
            }
        }
        return best;
    }

    private int compareLeaderboardRecord(ExamRecord left, ExamRecord right) {
        double leftScore = left.getTotalScore() == null ? 0.0 : left.getTotalScore();
        double rightScore = right.getTotalScore() == null ? 0.0 : right.getTotalScore();
        int scoreCompare = Double.compare(rightScore, leftScore);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        LocalDateTime leftTime = left.getSubmitTime() == null ? LocalDateTime.MAX : left.getSubmitTime();
        LocalDateTime rightTime = right.getSubmitTime() == null ? LocalDateTime.MAX : right.getSubmitTime();
        return leftTime.compareTo(rightTime);
    }

    private List<LeaderboardItemDTO> toLeaderboard(Collection<ExamRecord> records) {
        List<ExamRecord> sorted = records.stream()
                .sorted(this::compareLeaderboardRecord)
                .collect(Collectors.toList());
        Map<Integer, String> usernames = userRepository.findAllById(
                sorted.stream().map(ExamRecord::getStudentId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));
        List<LeaderboardItemDTO> items = new ArrayList<>();
        int rank = 1;
        for (ExamRecord record : sorted) {
            LeaderboardItemDTO item = new LeaderboardItemDTO();
            item.setRank(rank++);
            item.setUsername(usernames.getOrDefault(record.getStudentId(), "未知"));
            item.setScore(record.getTotalScore());
            item.setSubmitTime(record.getSubmitTime());
            items.add(item);
        }
        return items;
    }

    private Double average(List<ExamRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        return roundScore(records.stream()
                .map(ExamRecord::getTotalScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));
    }

    private List<PaperStatisticsDTO.StudentScoreDTO> toStudentScores(List<ExamRecord> records) {
        Map<Integer, String> usernames = userRepository.findAllById(
                records.stream().map(ExamRecord::getStudentId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));
        return records.stream()
                .sorted(this::compareLeaderboardRecord)
                .map(record -> {
                    PaperStatisticsDTO.StudentScoreDTO dto = new PaperStatisticsDTO.StudentScoreDTO();
                    dto.setStudentId(record.getStudentId());
                    dto.setUsername(usernames.getOrDefault(record.getStudentId(), "未知"));
                    dto.setScore(record.getTotalScore());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
