# Student Module Notes

## Reserved Paper Settings

The student module already reserves these paper-level fields for future teacher-side support:

- `maxAttempts`
- `openStartTime`
- `openEndTime`

Current compatibility defaults:

- `maxAttempts = 1`
- `openStartTime = null`
- `openEndTime = null`
- Null open time values are treated as open by default.

The student paper list response also exposes:

- `attemptCount`
- `remainingAttempts`
- `bestScore`
- `status`
- `statusText`

When the teacher module later adds create/edit support for `maxAttempts`, `openStartTime`, and `openEndTime`, the student service should read those values from `Paper` instead of using the temporary defaults.

## Question Visibility

Before an exam starts, student list/detail pages show paper metadata only. Questions are returned to the frontend after `POST /api/student/exam-records` creates an `ExamRecord`, or through `GET /api/student/exam-records/{recordId}` for an unsubmitted record owned by the current student.
