# Student Module Notes

## Paper Settings

The student module reads these paper-level fields from `papers`:

- `maxAttempts`
- `openStartTime`
- `openEndTime`
- `teacherOpenAnswer`

Current compatibility defaults:

- `maxAttempts = 1`
- `openStartTime = papers.open_start_time`
- `openEndTime = papers.open_end_time`
- `teacherOpenAnswer = papers.teacher_open_answer`
- Null open time values are treated as unrestricted by default.

The student paper list response also exposes:

- `attemptCount`
- `remainingAttempts`
- `bestScore`
- `status`
- `statusText`

`status` is derived from the current time and attempt count:

- `NOT_OPEN`: current time is before `openStartTime`.
- `OPEN`: current time is within the open window and attempts remain.
- `CLOSED`: current time is at or after `openEndTime`.
- `NO_ATTEMPTS`: attempts are exhausted while the paper is otherwise open.

Teacher-side create/edit screens are not implemented yet. Future teacher APIs should write `teacherOpenAnswer`, `openStartTime`, and `openEndTime` to the `papers` table.

## Question Visibility

Before an exam starts, student list/detail pages show paper metadata only. Questions are returned to the frontend after `POST /api/student/exam-records` creates an `ExamRecord`, or through `GET /api/student/exam-records/{recordId}` for an unsubmitted record owned by the current student.

## Answer Visibility

Student record detail answers remain hidden unless both conditions are true:

- `teacherOpenAnswer === true`
- The current time is at or after `openEndTime`

If `openEndTime` is null, the old compatibility rule is used: answers may be shown after submission or after `startTime + duration`.
