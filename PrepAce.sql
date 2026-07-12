USE master;
GO

IF DB_ID('PrepAce') IS NOT NULL
BEGIN
    ALTER DATABASE PrepAce SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE PrepAce;
END
GO

CREATE DATABASE PrepAce;
GO

USE PrepAce;
GO

-- ===========================================================================
-- 1. TẠO CẤU TRÚC BẢNG (SCHEMA)
-- ===========================================================================

CREATE TABLE Roles (
    role_id INT PRIMARY KEY IDENTITY(1,1),
    role_name NVARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE Users (
    user_id INT PRIMARY KEY IDENTITY(1,1),
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(100) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    phone NVARCHAR(20),
    avatar_url NVARCHAR(255),
    role_id INT NOT NULL,
    account_status NVARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT GETDATE(),
	verification_code VARCHAR(10),
	verification_expiry DATETIME,
	school NVARCHAR(255),
	bio NVARCHAR(MAX),
	role_name NVARCHAR(50),
	failed_attempts INT DEFAULT 0,
	lockout_expiry DATETIME NULL,
	otp_resend_count INT DEFAULT 0,
	otp_failed_attempts INT DEFAULT 0,
	change_pw_failed_attempts INT DEFAULT 0,
	change_pw_lockout_expiry DATETIME NULL,
	token_version INT DEFAULT 1,
	reset_token NVARCHAR(255) NULL,
	reset_token_expiry DATETIME NULL,

    FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

CREATE TABLE AuditLogs (
    log_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT,
    action_name NVARCHAR(255),
    action_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE Reports (
    report_id INT PRIMARY KEY IDENTITY(1,1),
    reporter_id INT NOT NULL,
    report_type NVARCHAR(100),
    report_content NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (reporter_id) REFERENCES Users(user_id)
);

CREATE TABLE Categories (
    category_id INT PRIMARY KEY IDENTITY(1,1),
    category_name NVARCHAR(100) NOT NULL
);

CREATE TABLE Subjects (
    subject_id INT PRIMARY KEY IDENTITY(1,1),
    subject_name NVARCHAR(100) NOT NULL,
    category_id INT,
    FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);

CREATE TABLE Courses (
    course_id INT PRIMARY KEY IDENTITY(1,1),
    teacher_id INT NOT NULL,
    subject_id INT NOT NULL,
    course_title NVARCHAR(255) NOT NULL,
    course_description NVARCHAR(MAX),
    thumbnail_url NVARCHAR(255),
    price DECIMAL(10,2) DEFAULT 0,
    is_published BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    status NVARCHAR(20) DEFAULT 'PENDING',
    review_note NVARCHAR(MAX),
    reviewed_at DATETIME,
    FOREIGN KEY (teacher_id) REFERENCES Users(user_id),
    FOREIGN KEY (subject_id) REFERENCES Subjects(subject_id)
);

CREATE TABLE Chapters (
    chapter_id INT PRIMARY KEY IDENTITY(1,1),
    chapter_order INT,
    chapter_title NVARCHAR(255) NOT NULL,
    course_id INT,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE Notifications (
    notification_id INT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    target_role NVARCHAR(20) DEFAULT 'ALL',
    created_at DATETIME DEFAULT GETDATE(),
    created_by INT,
    user_id INT NULL,
    is_read BIT DEFAULT 0,
    FOREIGN KEY (created_by) REFERENCES Users(user_id),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE Lessons (
    lesson_id INT PRIMARY KEY IDENTITY(1,1),
    lesson_title NVARCHAR(255) NOT NULL,
    lesson_description NVARCHAR(MAX),
    video_url NVARCHAR(255),
    subtitle_url NVARCHAR(255),
    lesson_order INT,
    created_at DATETIME DEFAULT GETDATE(),
    chapter_id INT,
    course_id INT,
    duration NVARCHAR(20) DEFAULT '00:00',
    is_preview BIT DEFAULT 0,
    FOREIGN KEY (chapter_id) REFERENCES Chapters(chapter_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE LearningMaterials (
    material_id INT PRIMARY KEY IDENTITY(1,1),
    lesson_id INT NOT NULL,
    material_title NVARCHAR(255),
    file_url NVARCHAR(255),
    uploaded_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (lesson_id) REFERENCES Lessons(lesson_id)
);

CREATE TABLE in_video_questions (
    id INT PRIMARY KEY IDENTITY(1,1),
    lesson_id INT NOT NULL,
    timestamp_seconds INT NOT NULL,
    question_text NVARCHAR(MAX) NOT NULL,
    option_a NVARCHAR(255) NOT NULL,
    option_b NVARCHAR(255) NOT NULL,
    option_c NVARCHAR(255) NOT NULL,
    option_d NVARCHAR(255) NOT NULL,
    correct_option VARCHAR(1) NOT NULL,
    FOREIGN KEY (lesson_id) REFERENCES Lessons(lesson_id)
);

CREATE TABLE Enrollments (
    enrollment_id INT PRIMARY KEY IDENTITY(1,1),
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    enrolled_at DATETIME DEFAULT GETDATE(),
    progress_percent FLOAT DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES Users(user_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE LessonProgress (
    progress_id INT PRIMARY KEY IDENTITY(1,1),
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    lesson_id INT NOT NULL,
    is_completed BIT NOT NULL DEFAULT 0,
    score FLOAT NULL,
    last_accessed DATETIME NULL,
    CONSTRAINT UQ_LessonProgress UNIQUE (student_id, lesson_id),
    FOREIGN KEY (student_id) REFERENCES Users(user_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id),
    FOREIGN KEY (lesson_id) REFERENCES Lessons(lesson_id)
);

CREATE TABLE Assignments (
    assignment_id INT PRIMARY KEY IDENTITY(1,1),
    course_id INT NOT NULL,
    assignment_title NVARCHAR(255),
    assignment_description NVARCHAR(MAX),
    due_date DATETIME,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE AssignmentSubmissions (
    submission_id INT PRIMARY KEY IDENTITY(1,1),
    assignment_id INT NOT NULL,
    student_id INT NOT NULL,
    submission_text NVARCHAR(MAX),
    file_url NVARCHAR(255),
    score FLOAT,
    feedback NVARCHAR(MAX),
    submitted_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (assignment_id) REFERENCES Assignments(assignment_id),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

CREATE TABLE Quizzes (
    quiz_id INT IDENTITY(1,1) PRIMARY KEY,
    course_id INT NULL,
    quiz_title NVARCHAR(500) NOT NULL,
    duration_minutes INT NOT NULL,
    quiz_type VARCHAR(50) DEFAULT 'PRACTICE',
    subject VARCHAR(50),
    is_entry_test BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE Questions (
    question_id INT IDENTITY(1,1) PRIMARY KEY,
    quiz_id INT,
    question_content NVARCHAR(MAX) NOT NULL,
    correct_answer NVARCHAR(255),
    explanation NVARCHAR(MAX),
    difficulty INT CHECK (difficulty BETWEEN 1 AND 4),
    topic NVARCHAR(200),
    subject VARCHAR(50),
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id)
);

CREATE TABLE QuestionOptions (
    option_id INT IDENTITY(1,1) PRIMARY KEY,
    question_id INT NOT NULL,
    option_content NVARCHAR(500) NOT NULL,
    is_correct BIT DEFAULT 0 NOT NULL,
    FOREIGN KEY (question_id) REFERENCES Questions(question_id)
);

CREATE TABLE QuizAttempts (
    attempt_id INT PRIMARY KEY IDENTITY(1,1),
    quiz_id INT NOT NULL,
    student_id INT NOT NULL,
    score FLOAT,
    started_at DATETIME DEFAULT GETDATE(),
    submitted_at DATETIME,
    time_spent INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

CREATE TABLE StudentAnswers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    selected_option_id INT NULL,
    answered_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (attempt_id) REFERENCES QuizAttempts(attempt_id),
    FOREIGN KEY (question_id) REFERENCES Questions(question_id),
    FOREIGN KEY (selected_option_id) REFERENCES QuestionOptions(option_id)
);

CREATE TABLE Payments (
    payment_id INT PRIMARY KEY IDENTITY(1,1),
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    amount DECIMAL(10,2),
    payment_method NVARCHAR(50),
    payment_status NVARCHAR(50),
    transaction_code NVARCHAR(100),
    paid_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (student_id) REFERENCES Users(user_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE CourseReviews (
    review_id INT PRIMARY KEY IDENTITY(1,1),
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment NVARCHAR(MAX),
    reviewed_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (student_id) REFERENCES Users(user_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

CREATE TABLE AIChatHistory (
    chat_id INT PRIMARY KEY IDENTITY(1,1),
    student_id INT NOT NULL,
    question NVARCHAR(MAX),
    ai_response NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    request_type VARCHAR(50),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

CREATE TABLE AcademicQuestions (
    question_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    lesson_id INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    timestamp_seconds INT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (lesson_id) REFERENCES Lessons(lesson_id)
);

CREATE TABLE AcademicAnswers (
    answer_id INT PRIMARY KEY IDENTITY(1,1),
    question_id INT NOT NULL,
    user_id INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (question_id) REFERENCES AcademicQuestions(question_id),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE student_notes (
    note_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT NOT NULL,
    lesson_id INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    timestamp_seconds INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (lesson_id) REFERENCES Lessons(lesson_id)
);
GO


-- ===========================================================================
-- 2. INSERT DỮ LIỆU CƠ BẢN (Roles, Users, Categories, Subjects, Courses)
-- ===========================================================================

INSERT INTO Roles(role_name) VALUES ('ADMIN'), ('TEACHER'), ('STUDENT');

INSERT INTO Users
(
    full_name,
    email,
    password_hash,
    phone,
    avatar_url,
    role_id,
    account_status,
    school,
    bio,
    verification_code,
    verification_expiry
)
VALUES

('System Administrator', 'admin@learnifyfuture.com', '123456', '0901111111', 'admin.jpg', 1, 'ACTIVE', NULL, NULL, NULL, NULL),

('Nguyen Minh Quan', 'teacher.math@learnify.com', '123456', '0902222222', 'teacher1.jpg', 2, 'ACTIVE', 'FPT University', 'Math teacher with 5 years experience', NULL, NULL),

('Tran Bao Chau', 'teacher.physics@learnify.com', '123456', '0903333333', 'teacher2.jpg', 2, 'ACTIVE', 'Hanoi University', 'Physics specialist', NULL, NULL),

('Le Hoang Nam', 'teacher.english@learnify.com', '123456', '0904444444', 'teacher3.jpg', 2, 'ACTIVE', 'University of Languages', 'English teacher IELTS 8.0', NULL, NULL),

('Pham Duc Anh', 'student1@gmail.com', '123456', '0905555555', 'student1.jpg', 3, 'ACTIVE', 'THPT Chu Van An', 'Student interested in science', NULL, NULL),

('Vo Minh Tri', 'student2@gmail.com', '123456', '0906666666', 'student2.jpg', 3, 'ACTIVE', 'THPT Le Quy Don', 'Good at math and physics', NULL, NULL),

('Nguyen Thanh Dat', 'student3@gmail.com', '123456', '0907777777', 'student3.jpg', 3, 'ACTIVE', 'THPT Nguyen Hue', 'Preparing for university exam', NULL, NULL);

UPDATE Users SET role_name = 'ADMIN' WHERE role_id = 1;
UPDATE Users SET role_name = 'TEACHER' WHERE role_id = 2;
UPDATE Users SET role_name = 'STUDENT' WHERE role_id = 3;

INSERT INTO Categories(category_name) VALUES ('Natural Sciences'), ('Social Sciences'), ('Languages'), ('University Preparation');

INSERT INTO Subjects(subject_name, category_id) VALUES
('Mathematics', 1), ('Physics', 1), ('Chemistry', 1), ('Literature', 2), ('English', 3), ('History', 2), ('Geography', 2);

INSERT INTO Courses (teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, status) VALUES
(2, 1, 'Mastering Mathematics 12', 'Complete mathematics course for National High School Exam preparation.', '/uploads/thumbnails/math-course.jpg', 599000, 1, 'PUBLISHED'),
(3, 2, 'Physics Problem Solving Techniques', 'Advanced physics lessons and mock exam strategies.', '/uploads/thumbnails/physics-course.jpg', 499000, 1, 'PUBLISHED'),
(4, 5, 'English Vocabulary & Grammar', 'Comprehensive English preparation for university entrance exam.', '/uploads/thumbnails/english-course.jpg', 399000, 1, 'PUBLISHED'),
(2, 1, N'Tuyệt đỉnh Casio - Giải nhanh trắc nghiệm Toán', 'Bí kíp sử dụng máy tính Casio Fx-580VNX & Fx-880BTG cho kỳ thi THPT.', '/uploads/thumbnails/casio-course.jpg', 299000, 0, 'PENDING');

-- ===========================================================================
-- 3. INSERT CHAPTERS & LESSONS
-- ===========================================================================

SET IDENTITY_INSERT Chapters ON;
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id) VALUES 
(1, 1, N'Chapter 1: Derivatives & Integrals', 1),
(2, 1, N'Chapter 1: Mechanics', 2),
(3, 1, N'Chapter 1: Foundations', 3),
(14, 3, N'Chapter 3: Hàm số luỹ thừa', 1),
(16, 4, N'Chapter3: Xác xuất thống kê', 1);
SET IDENTITY_INSERT Chapters OFF;

SET IDENTITY_INSERT Lessons ON;
INSERT INTO Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, chapter_id, course_id, duration) VALUES 
(1, N'Derivative Basics', N'Introduction to derivatives and formulas.', N'https://www.youtube.com/watch?v=hiWjWvba1Kc', N'/uploads/subtitles/math/derivative-basics.vtt', 1, 1, 1, N'11:38'),
(2, N'Applications of Derivatives', N'Optimization and graph analysis.', N'https://www.youtube.com/watch?v=Oa-mVxGS4cw', N'/uploads/subtitles/math/applications-derivatives.vtt', 2, 1, 1, N'22:44'),
(3, N'Integral Fundamentals', N'Basic integration techniques.', N'https://www.youtube.com/watch?v=ndIRu-bUBx0', N'/uploads/subtitles/math/integral-fundamentals.vtt', 3, 1, 1, N'03:42'),
(4, N'Newton Laws of Motion', N'Force and motion concepts.', N'/uploads/videos/physics/newton-laws.mp4', N'/uploads/subtitles/physics/newton-laws.vtt', 1, 2, 2, N'15:30'),
(5, N'Circular Motion', N'Uniform circular motion formulas.', N'/uploads/videos/physics/circular-motion.mp4', N'/uploads/subtitles/physics/circular-motion.vtt', 2, 2, 2, N'20:00'),
(6, N'Grammar Basics', N'English grammar foundation.', N'/uploads/videos/english/grammar-basics.mp4', N'/uploads/subtitles/english/grammar-basics.vtt', 1, 3, 3, N'15:30'),
(7, N'Vocabulary Building', N'Vocabulary improvement methods.', N'/uploads/videos/english/vocabulary-building.mp4', N'/uploads/subtitles/english/vocabulary-building.vtt', 2, 3, 3, N'20:00');
SET IDENTITY_INSERT Lessons OFF;

INSERT INTO LearningMaterials (lesson_id, material_title, file_url) VALUES
(1, 'Derivative Formula Summary', '/uploads/documents/math/derivative-summary.pdf'),
(2, 'Optimization Exercises', '/uploads/documents/math/optimization.docx'),
(3, 'Integral Practice Sheet', '/uploads/documents/math/integral-practice.pdf'),
(4, 'Newton Laws Summary', '/uploads/documents/physics/newton-summary.pdf'),
(5, 'Circular Motion Exercises', '/uploads/documents/physics/circular-motion.pdf'),
(6, 'Grammar Handbook', '/uploads/documents/english/grammar-handbook.pdf'),
(7, 'Vocabulary Workbook', '/uploads/documents/english/vocabulary-workbook.pdf');

-- ===========================================================================
-- 4. ENROLLMENTS & ASSIGNMENTS
-- ===========================================================================

INSERT INTO Enrollments (student_id, course_id, progress_percent) VALUES
(5, 1, 75), (5, 2, 50), (6, 1, 90), (6, 3, 65), (7, 2, 40), (7, 3, 85);

INSERT INTO Assignments (course_id, assignment_title, assignment_description, due_date) VALUES
(1, 'Derivative Homework', 'Complete derivative exercises from chapter 1.', '2026-06-30'),
(2, 'Physics Force Assignment', 'Solve force and motion problems.', '2026-07-05'),
(3, 'English Essay Writing', 'Write a short essay about education.', '2026-07-10');

INSERT INTO AssignmentSubmissions (assignment_id, student_id, submission_text, file_url, score, feedback) VALUES
(1, 5, 'Completed all derivative questions.', '/uploads/submissions/math/student5-derivative.pdf', 8.5, 'Good understanding, improve presentation.'),
(2, 6, 'Physics assignment completed.', '/uploads/submissions/physics/student6-force.pdf', 9.0, 'Excellent calculations.'),
(3, 7, 'Essay submitted successfully.', '/uploads/submissions/english/student7-essay.docx', 8.0, 'Grammar needs slight improvement.');

-- ===========================================================================
-- 5. QUIZZES
-- ===========================================================================

SET IDENTITY_INSERT Quizzes ON;
INSERT INTO Quizzes (quiz_id, course_id, quiz_title, duration_minutes, quiz_type, subject, is_entry_test) VALUES
(1, 1, N'Đề thi thử Toán THPT Quốc gia 2026 - Đề 1', 90, 'ENTRY_TEST', 'math', 1),
(2, 1, N'Bài tập trắc nghiệm Đạo hàm và Tích phân', 60, 'PRACTICE', 'math', 0),
(3, 2, N'Đề thi thử Vật Lý THPT Quốc gia 2026', 50, 'ENTRY_TEST', 'physics', 1),
(4, 2, N'Bài tập cơ học và Động lực học', 45, 'PRACTICE', 'physics', 0),
(5, 3, N'Đề thi thử Tiếng Anh THPT - Reading & Grammar', 50, 'ENTRY_TEST', 'english', 1),
(6, 3, N'Bài kiểm tra từ vựng và cấu trúc câu', 40, 'PRACTICE', 'english', 0);
SET IDENTITY_INSERT Quizzes OFF;

-- ===========================================================================
-- 6. CÂU HỎI & ĐÁP ÁN (Dùng Variables tránh lỗi trùng ID)
-- ===========================================================================
GO
--DECLARE @QuizId INT;
--DECLARE @QId INT;

-- ---------------------------------------------------------
-- QUIZ 1: Đề thi thử Toán Đề 1
-- ---------------------------------------------------------
SET @QuizId = 1;

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Tìm đạo hàm của hàm số y = x^3 - 3x.', '3x^2 - 3', N'y'' = (x^3)'' - (3x)'' = 3x^2 - 3'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'3x^2 - 3'), (@QId, N'3x^2'), (@QId, N'x^2 - 3'), (@QId, N'3x^2 + 3');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Tìm đường tiệm cận đứng của đồ thị hàm số y = (2x + 1)/(x - 1).', 'x = 1', N'Tiệm cận đứng là nghiệm của mẫu số: x - 1 = 0 => x = 1'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'x = 1'), (@QId, N'y = 2'), (@QId, N'x = -1'), (@QId, N'y = -1');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Hàm số y = x^4 - 2x^2 đạt cực tiểu tại điểm nào?', 'x = 1 và x = -1', N'y'' = 4x^3 - 4x = 0 => x = 0, x = 1, x = -1. Lập bảng biến thiên ta thấy x = 1 và x = -1 là điểm cực tiểu.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'x = 1 và x = -1'), (@QId, N'x = 0'), (@QId, N'x = 2'), (@QId, N'x = -2');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Tìm nguyên hàm của hàm số f(x) = cos(x).', 'sin(x) + C', N'Theo bảng nguyên hàm cơ bản, nguyên hàm của cos(x) là sin(x) + C'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'sin(x) + C'), (@QId, N'-sin(x) + C'), (@QId, N'tan(x) + C'), (@QId, N'-cos(x) + C');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Tính tích phân I = tích phân từ 0 đến 1 của e^x dx.', 'e - 1', N'I = e^x thế số từ 0 đến 1 = e^1 - e^0 = e - 1'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'e - 1'), (@QId, N'e'), (@QId, N'e + 1'), (@QId, N'1');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Tính thể tích V của khối lập phương có cạnh bằng 2a.', '8a^3', N'V = cạnh^3 = (2a)^3 = 8a^3'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'8a^3'), (@QId, N'2a^3'), (@QId, N'4a^3'), (@QId, N'a^3');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Công thức tính thể tích V của khối chóp có diện tích đáy B và chiều cao h là gì?', 'V = (1/3)Bh', N'Thể tích khối chóp bằng một phần ba tích diện tích đáy và chiều cao.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'V = (1/3)Bh'), (@QId, N'V = Bh'), (@QId, N'V = 3Bh'), (@QId, N'V = (1/2)Bh');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Giải phương trình log3(x - 1) = 2.', 'x = 10', N'Điều kiện x > 1. Ta có: x - 1 = 3^2 = 9 => x = 10'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'x = 10'), (@QId, N'x = 7'), (@QId, N'x = 8'), (@QId, N'x = 9');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Giải phương trình mũ: 2^(x + 1) = 8.', 'x = 2', N'2^(x + 1) = 2^3 => x + 1 = 3 => x = 2'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'x = 2'), (@QId, N'x = 3'), (@QId, N'x = 1'), (@QId, N'x = 4');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Cho số phức z = 3 + 4i. Tính môđun của số phức z.', '5', N'|z| = căn(3^2 + 4^2) = 5'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'5'), (@QId, N'7'), (@QId, N'25'), (@QId, N'căn(7)');

-- ---------------------------------------------------------
-- QUIZ 3: Đề thi thử Vật Lý
-- ---------------------------------------------------------
DECLARE @QuizId INT;
DECLARE @QId INT;
SET @QuizId = 3;

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Một con lắc lò xo gồm lò xo có độ cứng k và vật nhỏ có khối lượng m. Chu kỳ dao động điều hòa của con lắc được tính bằng công thức nào?', 'T = 2*pi*căn(m/k)', N'Chu kỳ dao động của con lắc lò xo là T = 2*pi*căn(m/k).'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'T = 2*pi*căn(m/k)'), (@QId, N'T = 2*pi*căn(k/m)'), (@QId, N'T = 1/(2*pi)*căn(m/k)'), (@QId, N'T = 1/(2*pi)*căn(k/m)');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Một con lắc đơn có chiều dài l dao động điều hòa tại nơi có gia tốc trọng trường g. Tần số góc omega của con lắc được tính bằng công thức nào?', 'omega = căn(g/l)', N'Tần số góc của con lắc đơn được xác định bởi công thức omega = căn(g/l).'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'omega = căn(g/l)'), (@QId, N'omega = căn(l/g)'), (@QId, N'omega = 2*pi*căn(g/l)'), (@QId, N'omega = 1/(2*pi)*căn(g/l)');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Lực kéo về tác dụng lên một vật dao động điều hòa có đặc điểm nào sau đây?', N'Luôn hướng về vị trí cân bằng', N'Lực kéo về (hay lực phục hồi) luôn hướng về vị trí cân bằng.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES ( @QId, N'Luôn hướng về vị trí cân bằng'), (@QId, N'Luôn hướng về vị trí biên dương'), (@QId, N'Có độ lớn không đổi theo thời gian'), (@QId, N'Luôn cùng chiều với vectơ vận tốc');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Biên độ của dao động cưỡng bức không phụ thuộc vào đại lượng nào sau đây?', N'Pha ban đầu của ngoại lực tuần hoàn', N'Không phụ thuộc vào pha ban đầu.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'Pha ban đầu của ngoại lực tuần hoàn'), (@QId, N'Biên độ của ngoại lực tuần hoàn'), (@QId, N'Tần số của ngoại lực tuần hoàn'), (@QId, N'Lực cản của môi trường');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Một sóng cơ truyền trong một môi trường với tốc độ v và tần số f. Bước sóng lamda được tính bằng công thức nào?', 'lamda = v / f', N'lamda = v*T = v/f.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'lamda = v / f'), (@QId, N'lamda = v * f'), (@QId, N'lamda = f / v'), (@QId, N'lamda = 2*pi*v/f');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Khi nói về sóng âm, phát biểu nào sau đây là SAI?', N'Sóng âm truyền được trong chân không', N'Sóng âm là sóng cơ, không truyền được trong chân không.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'Sóng âm truyền được trong chân không'), (@QId, N'Sóng âm truyền được trong chất rắn'), (@QId, N'Sóng âm truyền được trong chất lỏng'), (@QId, N'Sóng âm không truyền được trong chân không');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Tai con người có thể nghe được những âm thanh có tần số nằm trong khoảng nào?', N'Từ 16 Hz đến 20000 Hz', N'Dưới 16 Hz là hạ âm, trên 20000 Hz là siêu âm.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'Từ 16 Hz đến 20000 Hz'), (@QId, N'Dưới 16 Hz'), (@QId, N'Trên 20000 Hz'), (@QId, N'Từ 0 Hz đến 16 Hz');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Công thức tính cảm kháng ZL của một cuộn cảm thuần có độ tự cảm L trong mạch điện xoay chiều tần số góc omega là:', 'ZL = omega * L', N'ZL = omega * L.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'ZL = omega * L'), (@QId, N'ZL = 1 / (omega * L)'), (@QId, N'ZL = căn(omega * L)'), (@QId, N'ZL = omega^2 * L');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Công thức tính dung kháng ZC của một tụ điện có điện dung C là:', 'ZC = 1 / (omega * C)', N'ZC = 1 / (omega * C).'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'ZC = 1 / (omega * C)'), (@QId, N'ZC = omega * C'), (@QId, N'ZC = omega / C'), (@QId, N'ZC = C / omega');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Hiện tượng cộng hưởng điện xảy ra trong mạch RLC nối tiếp khi nào?', 'ZL = ZC', N'Khi cảm kháng bằng dung kháng ZL = ZC.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'ZL = ZC'), (@QId, N'ZL > ZC'), (@QId, N'ZL < ZC'), (@QId, N'R = ZL');

-- ---------------------------------------------------------
-- QUIZ 4: Bài tập Cơ học và Động lực học
-- ---------------------------------------------------------
SET @QuizId = 4;

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Công thức tính tốc độ trung bình của một vật chuyển động là gì?', 'v = s / t', N'Tốc độ trung bình bằng quãng đường chia cho thời gian: v = s/t.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'v = s / t'), (@QId, N'v = s * t'), (@QId, N'v = t / s'), (@QId, N'v = 0.5 * s * t^2');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Trong chuyển động thẳng biến đổi đều, vận tốc tức thời v liên hệ với vận tốc ban đầu v0, gia tốc a và thời gian t theo công thức:', 'v = v0 + a*t', N'Phương trình vận tốc của chuyển động thẳng biến đổi đều.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'v = v0 + a*t'), (@QId, N'v = v0 - a*t'), (@QId, N'v = v0 + 0.5*a*t^2'), (@QId, N'v = a*t');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Phương trình quãng đường đi được s của một vật chuyển động thẳng biến đổi đều là:', 's = v0*t + 0.5*a*t^2', N'Công thức tính quãng đường.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N's = v0*t + 0.5*a*t^2'), (@QId, N's = v0*t + a*t^2'), (@QId, N's = v0 + a*t'), (@QId, N's = 0.5*a*t^2');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Hệ thức độc lập với thời gian là:', 'v^2 - v0^2 = 2*a*s', N'Công thức liên hệ không phụ thuộc vào thời gian t.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'v^2 - v0^2 = 2*a*s'), (@QId, N'v - v0 = 2*a*s'), (@QId, N'v^2 + v0^2 = 2*a*s'), (@QId, N'v^2 - v0^2 = a*s');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Một vật rơi tự do không vận tốc đầu từ độ cao h. Vận tốc v của vật ngay trước khi chạm đất là:', 'v = căn(2*g*h)', N'v = căn(2gh).'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'v = căn(2*g*h)'), (@QId, N'v = g*h'), (@QId, N'v = 2*g*h'), (@QId, N'v = căn(g*h)');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Định luật I Newton khẳng định:', N'Không chịu tác dụng của lực nào hoặc hợp lực bằng 0', N'Đó là nội dung định luật quán tính.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'Không chịu tác dụng của lực nào hoặc hợp lực bằng 0'), (@QId, N'Chỉ chịu tác dụng của lực ma sát'), (@QId, N'Chịu tác dụng của các lực không cân bằng'), (@QId, N'Vật có khối lượng rất lớn');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Biểu thức vectơ của Định luật II Newton là gì?', 'F = m * a', N'Gia tốc tỉ lệ thuận với lực tác dụng: F = m*a.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'F = m * a'), (@QId, N'F = m / a'), (@QId, N'F = a / m'), (@QId, N'F = m * a^2');

-- ---------------------------------------------------------
-- QUIZ 5: Đề thi thử Tiếng Anh
-- ---------------------------------------------------------
SET @QuizId = 5;

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'She _____ to the cinema with her friends last night.', 'went', N'Dấu hiệu "last night" chia thì quá khứ đơn (V2/ed của go là went).'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'went'), (@QId, N'goes'), (@QId, N'has gone'), (@QId, N'was going');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'The old house _____ by the storm yesterday.', 'was destroyed', N'Chủ ngữ vật, có "by" và "yesterday" -> Bị động quá khứ đơn.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'was destroyed'), (@QId, N'destroyed'), (@QId, N'is destroyed'), (@QId, N'was destroying');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'If I _____ you, I would accept that job offer.', 'were', N'Câu điều kiện loại 2. Động từ tobe chia "were" cho tất cả các ngôi.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'were'), (@QId, N'am'), (@QId, N'had been'), (@QId, N'will be');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'The man _____ lives next door is a famous musician.', 'who', N'Đại từ quan hệ thay thế cho danh từ chỉ người làm chủ ngữ.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'who'), (@QId, N'whom'), (@QId, N'which'), (@QId, N'whose');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'This smartphone is much _____ than my old one.', 'more expensive', N'So sánh hơn của tính từ dài.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'more expensive'), (@QId, N'expensive'), (@QId, N'most expensive'), (@QId, N'as expensive');

-- ---------------------------------------------------------
-- QUIZ 6: Từ vựng và cấu trúc câu
-- ---------------------------------------------------------
SET @QuizId = 6;

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'I am looking forward to _____ from you soon.', 'hearing', N'look forward to + V-ing'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'hearing'), (@QId, N'hear'), (@QId, N'heard'), (@QId, N'to hear');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'You need to _____ a decision right now before it''s too late.', 'make', N'make a decision (đưa ra quyết định).'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'make'), (@QId, N'do'), (@QId, N'take'), (@QId, N'give');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Effective _____ is the key to a successful relationship.', 'communication', N'Cần danh từ -> communication.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'communication'), (@QId, N'communicate'), (@QId, N'communicative'), (@QId, N'communicated');

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES (@QuizId, N'Despite the difficulties, he remained _____ about the future.', 'optimistic', N'Optimistic (lạc quan) phù hợp ngữ cảnh.'); SET @QId = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content) VALUES (@QId, N'optimistic'), (@QId, N'pessimistic'), (@QId, N'hopeless'), (@QId, N'disappointed');

GO

-- ===========================================================================
-- 7. QUIZ 2: Câu hỏi (Có cờ is_correct trong bảng Options theo Schema Mới)
-- ===========================================================================

DECLARE @QuizToan INT = (SELECT quiz_id FROM Quizzes WHERE quiz_title = N'Bài tập trắc nghiệm Đạo hàm và Tích phân');
DECLARE @StartQId INT;

-- Insert 30 câu hỏi
INSERT INTO Questions (quiz_id, question_content, explanation, difficulty, topic, subject) VALUES
(@QuizToan, N'Giá trị lim (x→0) sin(3x)/x là bao nhiêu?', N'Áp dụng công thức lim (x→0) sin(kx)/x = k. Với k = 3 nên kết quả là 3.', 2, N'Giới hạn', 'math'),
(@QuizToan, N'Đạo hàm của y = x^3 + 3x^2 - 5x + 7 là?', N'Đạo hàm từng hạng tử: 3x² + 6x - 5.', 1, N'Đạo hàm', 'math'),
(@QuizToan, N'Phương trình bậc hai ax² + bx + c = 0 có nghiệm kép khi?', N'Có nghiệm kép khi Δ = 0.', 2, N'Phương trình bậc hai', 'math'),
(@QuizToan, N'Diện tích hình tròn bán kính r là?', N'Công thức diện tích hình tròn là πr².', 1, N'Hình học', 'math'),
(@QuizToan, N'log₂(8) bằng bao nhiêu?', N'Vì 2³ = 8.', 1, N'Logarit', 'math'),
(@QuizToan, N'Đạo hàm của y = sin(2x) là?', N'Đạo hàm của sin(ax) là a·cos(ax).', 2, N'Đạo hàm', 'math'),
(@QuizToan, N'Giải phương trình x² - 5x + 6 = 0?', N'Phân tích thành (x-2)(x-3)=0.', 2, N'Phương trình bậc hai', 'math'),
(@QuizToan, N'Tích phân của x² dx là?', N'∫x²dx = x³/3 + C.', 2, N'Tích phân', 'math'),
(@QuizToan, N'Đường thẳng y = 2x + 3 cắt trục hoành tại?', N'Cho y = 0 ⇒ x = -3/2.', 2, N'Hàm số bậc nhất', 'math'),
(@QuizToan, N'Số nghiệm của sin(x)=0 trong [0,2π] là?', N'Có 3 nghiệm: 0, π, 2π.', 3, N'Lượng giác', 'math'),
(@QuizToan, N'Đạo hàm bậc hai của y=x⁴ là?', N'Đạo hàm lần 1 là 4x³, lần 2 là 12x².', 2, N'Đạo hàm', 'math'),
(@QuizToan, N'Giá trị C(5,2) là?', N'C(5,2)=10.', 2, N'Tổ hợp', 'math'),
(@QuizToan, N'Phương trình tiếp tuyến của y=x² tại x=2 là?', N'Tiếp tuyến có phương trình y=4x−4.', 3, N'Đạo hàm', 'math'),
(@QuizToan, N'Tập nghiệm của x²−4x+3>0 là?', N'Nghiệm nằm ngoài đoạn [1,3].', 3, N'Bất phương trình', 'math'),
(@QuizToan, N'log₃(27) bằng bao nhiêu?', N'3³=27.', 1, N'Logarit', 'math'),
(@QuizToan, N'Diện tích hình chữ nhật dài 5 rộng 3 là?', N'5×3=15.', 1, N'Hình học', 'math'),
(@QuizToan, N'Đường chéo hình vuông cạnh a là?', N'Áp dụng định lý Pythagoras.', 2, N'Hình học', 'math'),
(@QuizToan, N'Giá trị nhỏ nhất của y=x²−4x+5 là?', N'Viết thành (x−2)²+1.', 3, N'Hàm số bậc hai', 'math'),
(@QuizToan, N'cos(60°) bằng bao nhiêu?', N'cos60°=1/2.', 1, N'Lượng giác', 'math'),
(@QuizToan, N'Thể tích hình cầu bán kính r là?', N'Công thức V=(4/3)πr³.', 2, N'Hình học', 'math'),
(@QuizToan, N'Số nguyên tố nhỏ nhất lớn hơn 20 là?', N'23 là số nguyên tố.', 1, N'Số học', 'math'),
(@QuizToan, N'Giải |x−2|<3?', N'Nghiệm là −1<x<5.', 2, N'Giá trị tuyệt đối', 'math'),
(@QuizToan, N'Đạo hàm của e^(2x) là?', N'Đạo hàm là 2e^(2x).', 2, N'Đạo hàm', 'math'),
(@QuizToan, N'Tổng các nghiệm của x³−6x²+11x−6=0 là?', N'Theo Viète bằng 6.', 3, N'Định lý Viète', 'math'),
(@QuizToan, N'Diện tích tam giác đáy 6 cao 4 là?', N'(6×4)/2=12.', 1, N'Hình học', 'math'),
(@QuizToan, N'Giá trị của 2¹⁰ là?', N'2¹⁰=1024.', 1, N'Lũy thừa', 'math'),
(@QuizToan, N'Phương trình x²+1=0 có nghiệm thực không?', N'Δ<0 nên vô nghiệm thực.', 2, N'Phương trình bậc hai', 'math'),
(@QuizToan, N'Parabol y=ax²+bx+c qua (1,3) và (2,7). Tìm a+b+c?', N'Từ dữ kiện suy ra a+b+c=5.', 3, N'Hàm số bậc hai', 'math'),
(@QuizToan, N'Tích hai nghiệm của x²−7x+12=0 là?', N'Theo Viète bằng 12.', 2, N'Định lý Viète', 'math'),
(@QuizToan, N'Giá trị nhỏ nhất của y=x²−6x+10 là?', N'Viết thành (x−3)²+1.', 3, N'Hàm số bậc hai', 'math');

-- Lấy ID của câu đầu tiên vừa được insert vào cho QuizToan
SET @StartQId = (SELECT MIN(question_id) FROM Questions WHERE quiz_id = @QuizToan);

-- Insert Options với cờ is_correct (Sử dụng ID tính toán động để không bị lệch)
INSERT INTO QuestionOptions (question_id, option_content, is_correct) VALUES
(@StartQId + 0, N'1', 0), (@StartQId + 0, N'2', 0), (@StartQId + 0, N'3', 1), (@StartQId + 0, N'6', 0),
(@StartQId + 1, N'3x² + 5x', 0), (@StartQId + 1, N'6x² - 5', 0), (@StartQId + 1, N'3x² + 6x - 5', 1), (@StartQId + 1, N'x² + 6x - 5', 0),
(@StartQId + 2, N'Δ > 0', 0), (@StartQId + 2, N'Δ = 0', 1), (@StartQId + 2, N'Δ < 0', 0), (@StartQId + 2, N'a = 0', 0),
(@StartQId + 3, N'2πr', 0), (@StartQId + 3, N'πr²', 1), (@StartQId + 3, N'πd', 0), (@StartQId + 3, N'r²', 0),
(@StartQId + 4, N'2', 0), (@StartQId + 4, N'4', 0), (@StartQId + 4, N'3', 1), (@StartQId + 4, N'8', 0),
(@StartQId + 5, N'2sin(2x)', 0), (@StartQId + 5, N'cos(2x)', 0), (@StartQId + 5, N'2cos(2x)', 1), (@StartQId + 5, N'sin(2x)', 0),
(@StartQId + 6, N'x = 2 hoặc x = 3', 1), (@StartQId + 6, N'x = 1 hoặc x = 6', 0), (@StartQId + 6, N'x = 2', 0), (@StartQId + 6, N'x = 3', 0),
(@StartQId + 7, N'x² + C', 0), (@StartQId + 7, N'(x³)/3 + C', 1), (@StartQId + 7, N'3x² + C', 0), (@StartQId + 7, N'x³ + C', 0),
(@StartQId + 8, N'(3/2, 0)', 0), (@StartQId + 8, N'(-3/2, 0)', 1), (@StartQId + 8, N'(0, 3)', 0), (@StartQId + 8, N'(2, 3)', 0),
(@StartQId + 9, N'2', 0), (@StartQId + 9, N'3', 1), (@StartQId + 9, N'4', 0), (@StartQId + 9, N'1', 0),
(@StartQId + 10, N'12x²', 1), (@StartQId + 10, N'4x³', 0), (@StartQId + 10, N'6x²', 0), (@StartQId + 10, N'24x', 0),
(@StartQId + 11, N'5', 0), (@StartQId + 11, N'20', 0), (@StartQId + 11, N'10', 1), (@StartQId + 11, N'15', 0),
(@StartQId + 12, N'y = 2x', 0), (@StartQId + 12, N'y = 4x - 4', 1), (@StartQId + 12, N'y = 4x + 4', 0), (@StartQId + 12, N'y = x²', 0),
(@StartQId + 13, N'x < 1 hoặc x > 3', 1), (@StartQId + 13, N'1 < x < 3', 0), (@StartQId + 13, N'x > 1', 0), (@StartQId + 13, N'x < 3', 0),
(@StartQId + 14, N'9', 0), (@StartQId + 14, N'2', 0), (@StartQId + 14, N'3', 1), (@StartQId + 14, N'1', 0),
(@StartQId + 15, N'8', 0), (@StartQId + 15, N'15', 1), (@StartQId + 15, N'10', 0), (@StartQId + 15, N'12', 0),
(@StartQId + 16, N'a√2', 1), (@StartQId + 16, N'2a', 0), (@StartQId + 16, N'a²', 0), (@StartQId + 16, N'a/2', 0),
(@StartQId + 17, N'0', 0), (@StartQId + 17, N'1', 1), (@StartQId + 17, N'2', 0), (@StartQId + 17, N'3', 0),
(@StartQId + 18, N'0.5', 1), (@StartQId + 18, N'1', 0), (@StartQId + 18, N'√3/2', 0), (@StartQId + 18, N'0', 0),
(@StartQId + 19, N'πr³', 0), (@StartQId + 19, N'(4/3)πr³', 1), (@StartQId + 19, N'4πr²', 0), (@StartQId + 19, N'r³', 0),
(@StartQId + 20, N'21', 0), (@StartQId + 20, N'23', 1), (@StartQId + 20, N'25', 0), (@StartQId + 20, N'29', 0),
(@StartQId + 21, N'-1 < x < 5', 1), (@StartQId + 21, N'x > 5', 0), (@StartQId + 21, N'x < 2', 0), (@StartQId + 21, N'0 < x < 3', 0),
(@StartQId + 22, N'e^(2x)', 0), (@StartQId + 22, N'2e^(2x)', 1), (@StartQId + 22, N'2x·e', 0), (@StartQId + 22, N'e^x', 0),
(@StartQId + 23, N'3', 0), (@StartQId + 23, N'6', 1), (@StartQId + 23, N'11', 0), (@StartQId + 23, N'18', 0),
(@StartQId + 24, N'10', 0), (@StartQId + 24, N'24', 0), (@StartQId + 24, N'12', 1), (@StartQId + 24, N'20', 0),
(@StartQId + 25, N'512', 0), (@StartQId + 25, N'1000', 0), (@StartQId + 25, N'1024', 1), (@StartQId + 25, N'2048', 0),
(@StartQId + 26, N'Có 2 nghiệm', 0), (@StartQId + 26, N'Có 1 nghiệm', 0), (@StartQId + 26, N'Không có nghiệm thực', 1), (@StartQId + 26, N'x = 1', 0),
(@StartQId + 27, N'5', 1), (@StartQId + 27, N'3', 0), (@StartQId + 27, N'7', 0), (@StartQId + 27, N'10', 0),
(@StartQId + 28, N'7', 0), (@StartQId + 28, N'12', 1), (@StartQId + 28, N'5', 0), (@StartQId + 28, N'19', 0),
(@StartQId + 29, N'0', 0), (@StartQId + 29, N'1', 1), (@StartQId + 29, N'2', 0), (@StartQId + 29, N'4', 0);
GO


-- ===========================================================================
-- 8. QUIZ ATTEMPTS VÀ NOTIFICATIONS
-- ===========================================================================

INSERT INTO QuizAttempts (quiz_id, student_id, score, status, started_at, submitted_at) VALUES
(1, 5, 8.5, 'COMPLETED', GETDATE(), GETDATE()),
(2, 6, 9.0, 'COMPLETED', GETDATE(), GETDATE()),
(3, 7, 7.5, 'COMPLETED', GETDATE(), GETDATE());

INSERT INTO Notifications (title, content, target_role, created_by) VALUES
('📢 Cập nhật tính năng Lộ trình AI mới', 'Hệ thống vừa nâng cấp thuật toán phân tích năng lực. Truy cập tab Lộ trình AI để xem gợi ý học tập mới nhất dành cho bạn!', 'ALL', 1),
('⏰ Nhắc nhở lịch học', 'Bạn có lịch Luyện đề Toán số 1 vào lúc 19:00 tối nay. Nhớ chuẩn bị giấy nháp và máy tính Casio nhé!', 'STUDENT', 1),
('✅ Kết quả chấm bài', 'Giáo viên Nguyễn Minh Quân đã chấm xong bài tập "Derivative Homework" của bạn. Điểm: 8.5/10.', 'STUDENT', 2),
('❌ Khóa học bị từ chối xuất bản', 'Admin đã từ chối xuất bản khóa học của bạn. Lý do: Thiếu video giới thiệu chương 2. Vui lòng kiểm tra và chỉnh sửa lại.', 'TEACHER', 1),
('🎉 Khóa học mới được duyệt', 'Khóa học "Tuyệt đỉnh Casio" của bạn đã được Admin duyệt và xuất bản thành công!', 'TEACHER', 1);

-- Kiểm tra lại schema
PRINT '=== Hoàn tất quá trình tạo Data (Merged SQL) ===';
GO