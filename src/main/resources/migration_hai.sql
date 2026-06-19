-- ============================================================
-- Migration cho các UC của Nguyễn Văn Hải
-- Chạy trên DB PrepAce (SQL Server)
-- ============================================================

-- 1. Thêm cột quiz_type vào Quizzes (phân biệt ENTRY_TEST / PRACTICE / MOCK_EXAM)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Quizzes') AND name = 'quiz_type')
    ALTER TABLE Quizzes ADD quiz_type NVARCHAR(50) DEFAULT 'PRACTICE';
GO

-- Cập nhật các quiz hiện có thành PRACTICE (nếu cần seed ENTRY_TEST thêm thủ công)
UPDATE Quizzes SET quiz_type = 'PRACTICE' WHERE quiz_type IS NULL;
GO

-- 2. Thêm cột cognitive_level vào Questions (1-4)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Questions') AND name = 'cognitive_level')
    ALTER TABLE Questions ADD cognitive_level INT DEFAULT 1;
GO

-- 3. Thêm total_questions, correct_count vào QuizAttempts
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('QuizAttempts') AND name = 'total_questions')
    ALTER TABLE QuizAttempts ADD total_questions INT;
GO
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('QuizAttempts') AND name = 'correct_count')
    ALTER TABLE QuizAttempts ADD correct_count INT;
GO

-- 4. Thêm request_type vào AIChatHistory
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('AIChatHistory') AND name = 'request_type')
    ALTER TABLE AIChatHistory ADD request_type NVARCHAR(50) DEFAULT 'CHAT';
GO

-- ── Seed: Tạo 1 Entry Test mẫu ────────────────────────────────────────────────

-- Tạo Entry Test tổng quát (course_id NULL = standalone)
INSERT INTO Quizzes (course_id, quiz_title, duration_minutes, quiz_type)
VALUES (NULL, 'Kiểm tra đầu vào tổng quát THPT 2026', 30, 'ENTRY_TEST');
GO

DECLARE @quiz_id INT = SCOPE_IDENTITY();

-- Thêm câu hỏi mẫu
INSERT INTO Questions (quiz_id, question_content, correct_answer, cognitive_level) VALUES
(@quiz_id, 'Đạo hàm của hàm số f(x) = x² + 3x là?',       '2x + 3',  2),
(@quiz_id, 'Đơn vị đo lực trong hệ SI là gì?',             'Newton',   1),
(@quiz_id, 'Chọn câu đúng ngữ pháp tiếng Anh:',            'B',        2),
(@quiz_id, 'Vận tốc ánh sáng trong chân không xấp xỉ bao nhiêu?', '3×10⁸ m/s', 1),
(@quiz_id, 'Giải phương trình: 2x - 4 = 0',                'x = 2',    2);
GO

DECLARE @q1 INT, @q2 INT, @q3 INT, @q4 INT, @q5 INT;
SELECT @q1 = question_id FROM Questions WHERE question_content LIKE 'Đạo hàm%' AND quiz_id = (SELECT MAX(quiz_id) FROM Quizzes WHERE quiz_type = 'ENTRY_TEST');
SELECT @q2 = question_id FROM Questions WHERE question_content LIKE 'Đơn vị đo lực%';
SELECT @q3 = question_id FROM Questions WHERE question_content LIKE 'Chọn câu đúng%';
SELECT @q4 = question_id FROM Questions WHERE question_content LIKE 'Vận tốc ánh sáng%';
SELECT @q5 = question_id FROM Questions WHERE question_content LIKE 'Giải phương trình%';

-- Options cho câu 1
INSERT INTO QuestionOptions (question_id, option_content) VALUES
(@q1, '2x + 3'), (@q1, 'x² + 3'), (@q1, '2x'), (@q1, '3x + 1');

-- Options cho câu 2
INSERT INTO QuestionOptions (question_id, option_content) VALUES
(@q2, 'Newton'), (@q2, 'Pascal'), (@q2, 'Joule'), (@q2, 'Watt');

-- Options cho câu 3
INSERT INTO QuestionOptions (question_id, option_content) VALUES
(@q3, 'She go to school'), (@q3, 'She goes to school'), (@q3, 'She going school'), (@q3, 'She goed to school');

-- Options cho câu 4
INSERT INTO QuestionOptions (question_id, option_content) VALUES
(@q4, '3×10⁸ m/s'), (@q4, '3×10⁶ m/s'), (@q4, '1,5×10⁸ m/s'), (@q4, '3×10¹⁰ m/s');

-- Options cho câu 5
INSERT INTO QuestionOptions (question_id, option_content) VALUES
(@q5, 'x = 2'), (@q5, 'x = -2'), (@q5, 'x = 4'), (@q5, 'x = 1');
GO
