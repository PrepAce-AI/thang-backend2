/* =====================================================================
   PrepAce — Migration cho các tính năng của Nguyễn Văn Hải
   (#13 Entry Test, #14 Payment, #26-30 AI Interface)

   - Bổ sung các cột mà entity backend yêu cầu nhưng schema gốc còn thiếu
   - Tạo đề Kiểm tra đầu vào (ENTRY_TEST) + câu hỏi + đáp án
   - Seed QuizAttempts / Payments để các tính năng AI có dữ liệu thật

   An toàn để chạy nhiều lần (idempotent).
   ===================================================================== */
USE [PrepAce];
GO
SET NOCOUNT ON;
GO

/* ---------------------------------------------------------------------
   1. BỔ SUNG CỘT CÒN THIẾU
   --------------------------------------------------------------------- */
IF COL_LENGTH('dbo.Quizzes', 'quiz_type') IS NULL
    ALTER TABLE dbo.Quizzes ADD quiz_type NVARCHAR(50) NULL;
GO
IF COL_LENGTH('dbo.Questions', 'cognitive_level') IS NULL
    ALTER TABLE dbo.Questions ADD cognitive_level INT NULL;
GO
IF COL_LENGTH('dbo.QuizAttempts', 'total_questions') IS NULL
    ALTER TABLE dbo.QuizAttempts ADD total_questions INT NULL;
GO
IF COL_LENGTH('dbo.QuizAttempts', 'correct_count') IS NULL
    ALTER TABLE dbo.QuizAttempts ADD correct_count INT NULL;
GO
IF COL_LENGTH('dbo.AIChatHistory', 'request_type') IS NULL
    ALTER TABLE dbo.AIChatHistory ADD request_type NVARCHAR(50) NULL;
GO

/* Phân loại quiz: mặc định PRACTICE, một số là đề thi thử MOCK_EXAM */
UPDATE dbo.Quizzes SET quiz_type = 'PRACTICE' WHERE quiz_type IS NULL;
UPDATE dbo.Quizzes SET quiz_type = 'MOCK_EXAM'
    WHERE quiz_id IN (4, 6, 8) AND quiz_type <> 'ENTRY_TEST';
GO

/* Gán mức độ nhận thức (1-4) cho câu hỏi chưa có */
UPDATE dbo.Questions SET cognitive_level = ((question_id % 4) + 1)
    WHERE cognitive_level IS NULL;
GO

/* Backfill số liệu cho các attempt cũ (nếu có) để không bị NULL */
UPDATE dbo.QuizAttempts
   SET total_questions = ISNULL(total_questions, 10),
       correct_count   = ISNULL(correct_count, CAST(ROUND(ISNULL(score,5)/10.0*10, 0) AS INT))
 WHERE total_questions IS NULL OR correct_count IS NULL;
GO

/* ---------------------------------------------------------------------
   2. ĐỀ KIỂM TRA ĐẦU VÀO (ENTRY_TEST)
   Tạo 3 đề: Toán (course 1), Vật lý (course 2), Tiếng Anh (course 3).
   Mỗi đề 5 câu trắc nghiệm; correct_answer = nội dung phương án đúng.
   --------------------------------------------------------------------- */

/* Xóa dữ liệu entry test cũ (nếu chạy lại) để tránh trùng */
DELETE o FROM dbo.QuestionOptions o
  JOIN dbo.Questions q ON o.question_id = q.question_id
  JOIN dbo.Quizzes z   ON q.quiz_id = z.quiz_id
 WHERE z.quiz_type = 'ENTRY_TEST';
DELETE qa FROM dbo.QuizAttempts qa
  JOIN dbo.Quizzes z ON qa.quiz_id = z.quiz_id
 WHERE z.quiz_type = 'ENTRY_TEST';
DELETE q FROM dbo.Questions q
  JOIN dbo.Quizzes z ON q.quiz_id = z.quiz_id
 WHERE z.quiz_type = 'ENTRY_TEST';
DELETE FROM dbo.Quizzes WHERE quiz_type = 'ENTRY_TEST';
GO

DECLARE @mathQuiz INT, @phyQuiz INT, @engQuiz INT;

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type)
VALUES (1, N'Kiểm tra đầu vào — Toán học', 20, GETDATE(), 'ENTRY_TEST');
SET @mathQuiz = SCOPE_IDENTITY();

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type)
VALUES (2, N'Kiểm tra đầu vào — Vật lý', 20, GETDATE(), 'ENTRY_TEST');
SET @phyQuiz = SCOPE_IDENTITY();

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type)
VALUES (3, N'Kiểm tra đầu vào — Tiếng Anh', 20, GETDATE(), 'ENTRY_TEST');
SET @engQuiz = SCOPE_IDENTITY();

/* Helper inline: chèn 1 câu hỏi + 4 phương án, đánh dấu đáp án đúng */
DECLARE @qid INT;

/* ---- TOÁN ---- */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@mathQuiz, N'Đạo hàm của hàm số y = x² là?', N'2x', N'(x^n)'' = n·x^(n-1)', 1);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'2x'),(@qid,N'x'),(@qid,N'x²'),(@qid,N'2');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@mathQuiz, N'Nghiệm của phương trình x² - 5x + 6 = 0 là?', N'x = 2 hoặc x = 3', N'Phân tích (x-2)(x-3)=0', 2);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x = 2 hoặc x = 3'),(@qid,N'x = 1 hoặc x = 6'),(@qid,N'x = -2 hoặc x = -3'),(@qid,N'Vô nghiệm');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@mathQuiz, N'log₂(8) bằng bao nhiêu?', N'3', N'2³ = 8', 1);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3'),(@qid,N'2'),(@qid,N'4'),(@qid,N'8');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@mathQuiz, N'Tích phân của x² dx là?', N'x³/3 + C', N'∫xⁿ dx = x^(n+1)/(n+1)+C', 3);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x³/3 + C'),(@qid,N'2x + C'),(@qid,N'x³ + C'),(@qid,N'3x² + C');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@mathQuiz, N'Giá trị nhỏ nhất của y = x² - 4x + 5 là?', N'1', N'Đỉnh parabol tại x=2 cho y=1', 4);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'5'),(@qid,N'-1');

/* ---- VẬT LÝ ---- */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@phyQuiz, N'Đơn vị SI của lực là gì?', N'Newton', N'F đo bằng Newton (N)', 1);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Newton'),(@qid,N'Joule'),(@qid,N'Pascal'),(@qid,N'Watt');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@phyQuiz, N'Công thức tính gia tốc theo định luật II Newton?', N'a = F/m', N'F = m·a', 2);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'a = F/m'),(@qid,N'a = m/F'),(@qid,N'a = F·m'),(@qid,N'a = F + m');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@phyQuiz, N'Vận tốc trong chuyển động tròn đều có đặc điểm?', N'Độ lớn không đổi, hướng thay đổi', N'Vận tốc tiếp tuyến', 3);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Độ lớn không đổi, hướng thay đổi'),(@qid,N'Không đổi hoàn toàn'),(@qid,N'Độ lớn thay đổi'),(@qid,N'Bằng 0');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@phyQuiz, N'Đại lượng nào sau đây là vô hướng?', N'Khối lượng', N'Khối lượng chỉ có độ lớn', 2);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Khối lượng'),(@qid,N'Lực'),(@qid,N'Vận tốc'),(@qid,N'Gia tốc');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@phyQuiz, N'Một vật rơi tự do (g=10m/s²) sau 2s có vận tốc?', N'20 m/s', N'v = g·t = 10·2', 4);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'20 m/s'),(@qid,N'10 m/s'),(@qid,N'40 m/s'),(@qid,N'5 m/s');

/* ---- TIẾNG ANH ---- */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@engQuiz, N'She _____ to school every day.', N'goes', N'Ngôi thứ 3 số ít thêm "s"', 1);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'goes'),(@qid,N'go'),(@qid,N'going'),(@qid,N'gone');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@engQuiz, N'I have lived in Hanoi _____ 2015.', N'since', N'since + mốc thời gian', 2);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'since'),(@qid,N'for'),(@qid,N'from'),(@qid,N'at');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@engQuiz, N'This is the book _____ I borrowed.', N'that', N'Đại từ quan hệ chỉ vật', 2);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'that'),(@qid,N'who'),(@qid,N'where'),(@qid,N'whom');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@engQuiz, N'Choose the synonym of "happy".', N'joyful', N'happy ≈ joyful', 3);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'joyful'),(@qid,N'angry'),(@qid,N'tired'),(@qid,N'sad');

INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level)
VALUES (@engQuiz, N'If I _____ rich, I would travel the world.', N'were', N'Câu điều kiện loại 2', 4);
SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'were'),(@qid,N'am'),(@qid,N'was'),(@qid,N'will be');
GO

/* ---------------------------------------------------------------------
   3. SEED QuizAttempts cho học sinh demo (user_id = 5: student1@gmail.com)
   Tạo phổ điểm theo môn: Toán giỏi, Vật lý yếu, Tiếng Anh khá
   -> để các tính năng AI (gap diagnosis / forecast / adaptive) có dữ liệu.
   --------------------------------------------------------------------- */
DELETE FROM dbo.QuizAttempts WHERE student_id = 5;
GO

INSERT INTO dbo.QuizAttempts (quiz_id, student_id, score, started_at, submitted_at, total_questions, correct_count) VALUES
 /* Toán (course 1: quiz 1,5) — mạnh */
 (1, 5, 9.0, DATEADD(DAY,-20,GETDATE()), DATEADD(DAY,-20,GETDATE()), 10, 9),
 (5, 5, 8.5, DATEADD(DAY,-12,GETDATE()), DATEADD(DAY,-12,GETDATE()), 10, 9),
 (5, 5, 9.0, DATEADD(DAY,-4 ,GETDATE()), DATEADD(DAY,-4 ,GETDATE()), 10, 9),
 /* Vật lý (course 2: quiz 2,6,7) — yếu */
 (2, 5, 4.0, DATEADD(DAY,-18,GETDATE()), DATEADD(DAY,-18,GETDATE()), 10, 4),
 (6, 5, 4.5, DATEADD(DAY,-9 ,GETDATE()), DATEADD(DAY,-9 ,GETDATE()), 10, 5),
 (7, 5, 5.0, DATEADD(DAY,-2 ,GETDATE()), DATEADD(DAY,-2 ,GETDATE()), 10, 5),
 /* Tiếng Anh (course 3: quiz 3,8,9) — khá */
 (3, 5, 7.0, DATEADD(DAY,-15,GETDATE()), DATEADD(DAY,-15,GETDATE()), 10, 7),
 (8, 5, 7.5, DATEADD(DAY,-6 ,GETDATE()), DATEADD(DAY,-6 ,GETDATE()), 10, 8),
 (9, 5, 8.0, DATEADD(DAY,-1 ,GETDATE()), DATEADD(DAY,-1 ,GETDATE()), 10, 8);
GO

/* ---------------------------------------------------------------------
   4. SEED Payments (lịch sử giao dịch cho học sinh demo)
   --------------------------------------------------------------------- */
DELETE FROM dbo.Payments WHERE student_id = 5;
INSERT INTO dbo.Payments (student_id, course_id, amount, payment_method, payment_status, transaction_code, paid_at)
VALUES (5, 1, 599000, N'VNPAY', N'SUCCESS', N'TXN-DEMO0001', DATEADD(DAY,-20,GETDATE())),
       (5, 2, 499000, N'MOMO' , N'SUCCESS', N'TXN-DEMO0002', DATEADD(DAY,-18,GETDATE()));
GO

PRINT '✅ Migration PrepAce (Hải features) hoàn tất.';
GO
