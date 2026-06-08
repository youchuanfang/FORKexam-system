package com.exam.service.student;

import com.exam.common.UserContext;
import com.exam.dto.common.ClassRoomDTO;
import com.exam.dto.common.LeaderboardItemDTO;
import com.exam.dto.common.StudentDTO;
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
import com.exam.entity.ClassMember;
import com.exam.entity.ClassRoom;
import com.exam.entity.ExamRecord;
import com.exam.entity.Paper;
import com.exam.entity.PaperQuestion;
import com.exam.entity.PaperTarget;
import com.exam.entity.Question;
import com.exam.entity.User;
import com.exam.repository.AnswerDetailRepository;
import com.exam.repository.ClassMemberRepository;
import com.exam.repository.ClassRoomRepository;
import com.exam.repository.ExamRecordRepository;
import com.exam.repository.PaperQuestionRepository;
import com.exam.repository.PaperRepository;
import com.exam.repository.PaperTargetRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.UserRepository;
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
    private final ClassRoomRepository classRoomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final PaperTargetRepository paperTargetRepository;
    private final UserRepository userRepository;

    public StudentServiceImpl(PaperRepository paperRepository,
                              PaperQuestionRepository paperQuestionRepository,
                              QuestionRepository questionRepository,
                              ExamRecordRepository examRecordRepository,
                              AnswerDetailRepository answerDetailRepository,
                              ClassRoomRepository classRoomRepository,
                              ClassMemberRepository classMemberRepository,
                              PaperTargetRepository paperTargetRepository,
                              UserRepository userRepository) {
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.examRecordRepository = examRecordRepository;
        this.answerDetailRepository = answerDetailRepository;
        this.classRoomRepository = classRoomRepository;
        this.classMemberRepository = classMemberRepository;
        this.paperTargetRepository = paperTargetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<PaperListDTO> getPapers() {
        Integer studentId = ensureStudent();
        List<ExamRecord> studentRecords = examRecordRepository.findByStudentId(studentId);
        return paperRepository.findAll().stream()
                .filter(paper -> isPaperEligibleForStudent(paper, studentId))
                .sorted(Comparator.comparing(Paper::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(paper -> buildPaperListDTO(paper, studentRecords))
                .collect(Collectors.toList());
    }

    @Override
    public PaperDetailDTO getPaperDetail(Integer paperId) {
        Integer studentId = ensureStudent();
        Paper paper = getPaperOrThrow(paperId);
        ensurePaperEligibleForStudent(paper, studentId);
        List<ExamRecord> studentRecords = examRecordRepository.findByStudentId(studentId);
        return buildPaperBasicDetail(paper, studentRecords);
    }

    @Override
    @Transactional
    public StartExamResponse startExam(StartExamRequest request) {
        Integer studentId = ensureStudent();
        if (request == null || request.getPaperId() == null) {
            throw new RuntimeException("paperId不能为空");
        }

        Paper paper = getPaperOrThrow(request.getPaperId());
        ensurePaperEligibleForStudent(paper, studentId);
        List<ExamRecord> studentRecords = examRecordRepository.findByStudentId(studentId);
        ExamRecord inProgress = findInProgressRecord(studentRecords, paper.getId());
        ensurePaperOpenForAnswering(paper);

        if (inProgress != null) {
            return buildStartExamResponse(paper, inProgress, true);
        }

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

        return buildStartExamResponse(paper, record, false);
    }

    @Override
    @Transactional
    public SubmitExamResponse submitExam(Integer recordId, SubmitExamRequest request) {
        Integer studentId = ensureStudent();
        ExamRecord record = getRecordOrThrow(recordId);
        ensureRecordOwner(record, studentId);
        if (record.getSubmitTime() != null) {
            throw new RuntimeException("该考试记录已提交，不能重复提交");
        }
        Paper paper = getPaperOrThrow(record.getPaperId());
        ensurePaperOpenForAnswering(paper);

        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByPaperId(record.getPaperId());
        if (paperQuestions.isEmpty()) {
            throw new RuntimeException("试卷没有题目，不能提交");
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
        response.setMessage("提交成功，客观题已自动判分，主观题待教师批改");
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

        boolean answerReleased = Boolean.TRUE.equals(paper.getReleaseAnswerFlag());
        boolean canReturnAnswers = answerReleased && isAnswerAvailable(paper);
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
        dto.setTeacherOpenAnswer(answerReleased);
        dto.setLeaderboardPublic(Boolean.TRUE.equals(paper.getLeaderboardPublic()));
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

    @Override
    public List<ClassRoomDTO> getMyClasses() {
        Integer studentId = ensureStudent();
        return classMemberRepository.findByStudentId(studentId).stream()
                .map(member -> classRoomRepository.findById(member.getClassId()).orElse(null))
                .filter(Objects::nonNull)
                .map(classRoom -> {
                    ClassRoomDTO dto = new ClassRoomDTO();
                    dto.setId(classRoom.getId());
                    dto.setName(classRoom.getName());
                    dto.setTeacherId(classRoom.getTeacherId());
                    dto.setJoinCode(classRoom.getJoinCode());
                    dto.setCreatedAt(classRoom.getCreatedAt());
                    dto.setMemberCount(classMemberRepository.findByClassId(classRoom.getId()).size());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassRoomDTO joinClass(String joinCode) {
        Integer studentId = ensureStudent();
        if (joinCode == null || joinCode.isBlank()) {
            throw new RuntimeException("邀请码不能为空");
        }
        ClassRoom classRoom = classRoomRepository.findByJoinCode(joinCode.trim())
                .orElseThrow(() -> new RuntimeException("班级不存在"));
        if (!classMemberRepository.existsByClassIdAndStudentId(classRoom.getId(), studentId)) {
            ClassMember member = new ClassMember();
            member.setClassId(classRoom.getId());
            member.setStudentId(studentId);
            member.setJoinedAt(LocalDateTime.now());
            classMemberRepository.save(member);
        }
        ClassRoomDTO dto = new ClassRoomDTO();
        dto.setId(classRoom.getId());
        dto.setName(classRoom.getName());
        dto.setTeacherId(classRoom.getTeacherId());
        dto.setJoinCode(classRoom.getJoinCode());
        dto.setCreatedAt(classRoom.getCreatedAt());
        dto.setMemberCount(classMemberRepository.findByClassId(classRoom.getId()).size());
        return dto;
    }

    @Override
    public List<LeaderboardItemDTO> getLeaderboard(Integer paperId) {
        Integer studentId = ensureStudent();
        Paper paper = getPaperOrThrow(paperId);
        ensurePaperEligibleForStudent(paper, studentId);
        if (!Boolean.TRUE.equals(paper.getLeaderboardPublic())) {
            throw new RuntimeException("排行榜未公开");
        }
        return toLeaderboard(bestSubmittedRecordsByStudent(paperId).values());
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
        dto.setTeacherOpenAnswer(Boolean.TRUE.equals(paper.getReleaseAnswerFlag()));
        dto.setInProgressRecordId(summary.getInProgressRecordId());
        dto.setLeaderboardPublic(Boolean.TRUE.equals(paper.getLeaderboardPublic()));
        dto.setQuestions(new ArrayList<>());
        return dto;
    }

    private StartExamResponse buildStartExamResponse(Paper paper, ExamRecord record, boolean resumed) {
        StartExamResponse response = new StartExamResponse();
        response.setRecordId(record.getId());
        response.setPaperId(paper.getId());
        response.setTitle(paper.getTitle());
        response.setStartTime(record.getStartTime());
        response.setOpenStartTime(paper.getOpenStartTime());
        response.setOpenEndTime(paper.getOpenEndTime());
        response.setDuration(paper.getDuration());
        response.setQuestions(buildQuestions(paper.getId()));
        response.setResumed(resumed);
        return response;
    }

    private List<QuestionDTO> buildQuestions(Integer paperId) {
        List<PaperQuestion> paperQuestions = paperQuestionRepository.findByPaperId(paperId);
        Map<Integer, Question> questionMap = questionRepository.findAllById(
                paperQuestions.stream().map(PaperQuestion::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Question::getId, Function.identity()));

        return paperQuestions.stream()
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
    }

    private PaperListDTO buildPaperListDTO(Paper paper, List<ExamRecord> studentRecords) {
        int maxAttempts = paper.getMaxAttempts() != null ? paper.getMaxAttempts() : 1;
        List<ExamRecord> recordsForPaper = studentRecords.stream()
                .filter(record -> Objects.equals(record.getPaperId(), paper.getId()))
                .collect(Collectors.toList());
        ExamRecord inProgress = findInProgressRecord(recordsForPaper, paper.getId());
        int attemptCount = recordsForPaper.size();
        int remainingAttempts = Math.max(0, maxAttempts - attemptCount);
        Double bestScore = recordsForPaper.stream()
                .filter(record -> record.getSubmitTime() != null)
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
        dto.setInProgressRecordId(inProgress == null ? null : inProgress.getId());
        dto.setLeaderboardPublic(Boolean.TRUE.equals(paper.getLeaderboardPublic()));
        LocalDateTime now = LocalDateTime.now();
        if (paper.getOpenStartTime() != null && now.isBefore(paper.getOpenStartTime())) {
            dto.setStatus("NOT_OPEN");
            dto.setStatusText("考试未开放");
        } else if (paper.getOpenEndTime() != null && !now.isBefore(paper.getOpenEndTime())) {
            dto.setStatus("CLOSED");
            dto.setStatusText("考试已截止");
        } else if (inProgress != null) {
            dto.setStatus("OPEN");
            dto.setStatusText("可继续作答");
        } else if (remainingAttempts <= 0) {
            dto.setStatus("NO_ATTEMPTS");
            dto.setStatusText("作答次数已用完");
        } else {
            dto.setStatus("OPEN");
            dto.setStatusText("考试开放中");
        }
        return dto;
    }

    private ExamRecord findInProgressRecord(List<ExamRecord> records, Integer paperId) {
        return records.stream()
                .filter(record -> Objects.equals(record.getPaperId(), paperId))
                .filter(record -> record.getSubmitTime() == null)
                .min(Comparator.comparing(ExamRecord::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .orElse(null);
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
                throw new RuntimeException("提交答案中存在缺少questionId的项目");
            }
            if (!paperQuestionIds.contains(answer.getQuestionId())) {
                throw new RuntimeException("提交答案中包含不属于当前试卷的题目");
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

    static String normalizeMultiChoiceValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String text = value.trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1);
        }
        text = text.replace("\"", "").replace("'", "");
        String[] parts = text.split("[,，;；\\s]+");
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            String item = part.trim().toUpperCase();
            if (!item.isEmpty()) {
                normalized.add(item);
            }
        }
        normalized.sort(String::compareTo);
        return String.join(",", normalized);
    }

    private String normalizeMultiChoice(String value) {
        return normalizeMultiChoiceValue(value);
    }

    private Integer ensureStudent() {
        Integer userId = UserContext.getUserId();
        String role = UserContext.getRole();
        if (userId == null) {
            throw new RuntimeException("未登录或token无效");
        }
        if (!ROLE_STUDENT.equals(role)) {
            throw new RuntimeException("仅学生角色可以访问该接口");
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

    private ExamRecord getRecordOrThrow(Integer recordId) {
        if (recordId == null) {
            throw new RuntimeException("recordId不能为空");
        }
        return examRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("考试记录不存在"));
    }

    private void ensureRecordOwner(ExamRecord record, Integer studentId) {
        if (!Objects.equals(record.getStudentId(), studentId)) {
            throw new RuntimeException("不能访问或提交别人的考试记录");
        }
    }

    private void ensurePaperOpenForAnswering(Paper paper) {
        LocalDateTime now = LocalDateTime.now();
        if (paper.getOpenStartTime() != null && now.isBefore(paper.getOpenStartTime())) {
            throw new RuntimeException("考试未开放");
        }
        if (paper.getOpenEndTime() != null && !now.isBefore(paper.getOpenEndTime())) {
            throw new RuntimeException("考试已截止");
        }
    }

    private ExamRecordDTO toExamRecordDTO(ExamRecord record, Paper paper) {
        ExamRecordDTO dto = new ExamRecordDTO();
        dto.setRecordId(record.getId());
        dto.setPaperId(record.getPaperId());
        dto.setPaperTitle(paper == null ? "未知试卷" : paper.getTitle());
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

    private boolean isAnswerAvailable(Paper paper) {
        if (!Boolean.TRUE.equals(paper.getReleaseAnswerFlag())) {
            return false;
        }
        return paper.getAnswerReleaseTime() == null
                || !LocalDateTime.now().isBefore(paper.getAnswerReleaseTime());
    }

    private boolean isPaperEligibleForStudent(Paper paper, Integer studentId) {
        if (!Boolean.TRUE.equals(paper.getPublished())) {
            return false;
        }
        List<PaperTarget> targets = paperTargetRepository.findByPaperId(paper.getId());
        if (targets.isEmpty()) {
            return false;
        }
        Set<Integer> classIds = classMemberRepository.findByStudentId(studentId).stream()
                .map(ClassMember::getClassId)
                .collect(Collectors.toSet());
        for (PaperTarget target : targets) {
            if (PaperTarget.TYPE_ALL.equals(target.getTargetType())) {
                return true;
            }
            if (PaperTarget.TYPE_STUDENT.equals(target.getTargetType()) && Objects.equals(target.getTargetId(), studentId)) {
                return true;
            }
            if (PaperTarget.TYPE_CLASS.equals(target.getTargetType()) && classIds.contains(target.getTargetId())) {
                return true;
            }
        }
        return false;
    }

    private void ensurePaperEligibleForStudent(Paper paper, Integer studentId) {
        if (!isPaperEligibleForStudent(paper, studentId)) {
            throw new RuntimeException("试卷未发布或无权限查看");
        }
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
