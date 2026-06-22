CREATE DATABASE PrepAce;
GO

USE PrepAce;
GO

-- =========================
-- USERS & ROLES
-- =========================

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
	role_name NVARCHAR(50)

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

-- ALTER TABLE Users ADD CONSTRAINT DF_Users_school DEFAULT NULL FOR school;
-- ALTER TABLE Users ADD CONSTRAINT DF_Users_bio DEFAULT NULL FOR bio;
-- Thêm cột role_name
-- ALTER TABLE Users ADD role_name NVARCHAR(50);

-- Cập nhật dữ liệu role_name dựa trên role_id hiện có --LƯU Ý: SAU KHI INSERT DATA Ở DƯỚI MỚI THÊM VÀO NHÁ

-- Kiểm tra
-- SELECT user_id, full_name, role_id, role_name FROM Users;
-- SELECT user_id, full_name, email, role_id, role_name, account_status
-- FROM Users
-- WHERE email = 'admin@learnifyfuture.com';
-- =========================
-- CATEGORIES & SUBJECTS
-- =========================

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

-- =========================
-- COURSES
-- =========================

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
	status NVARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PUBLISHED, REJECTED
    review_note NVARCHAR(MAX),
    reviewed_at DATETIME,

    FOREIGN KEY (teacher_id) REFERENCES Users(user_id),
    FOREIGN KEY (subject_id) REFERENCES Subjects(subject_id)
);

CREATE TABLE chapters (
    chapter_id INT PRIMARY KEY IDENTITY(1,1),
    chapter_order INT,
    chapter_title NVARCHAR(255) NOT NULL,
    course_id INT,
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

-- =========================
-- NOTIFICATION
-- =========================

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Notifications')
CREATE TABLE Notifications (
    notification_id INT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    target_role NVARCHAR(20) DEFAULT 'ALL', -- ALL, STUDENT, TEACHER
    created_at DATETIME DEFAULT GETDATE(),
    created_by INT
);

-- =========================
-- LESSONS
-- =========================

CREATE TABLE Lessons (
    lesson_id INT PRIMARY KEY IDENTITY(1,1),
    lesson_title NVARCHAR(255) NOT NULL,
    lesson_description NVARCHAR(MAX),
    video_url NVARCHAR(255),
    subtitle_url NVARCHAR(255),
    lesson_order INT,
    created_at DATETIME DEFAULT GETDATE(),
    chapter_id INT,
    course_id INT, -- Giữ cả 2 để tương thích cấu trúc file mới và cũ
    duration NVARCHAR(20) DEFAULT '00:00',
    is_preview BIT DEFAULT 0,
    FOREIGN KEY (chapter_id) REFERENCES chapters(chapter_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

-- =========================
-- LEARNING MATERIALS
-- =========================

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
    correct_option VARCHAR(1) NOT NULL,
    option_a NVARCHAR(255) NOT NULL,
    option_b NVARCHAR(255) NOT NULL,
    option_c NVARCHAR(255) NOT NULL,
    option_d NVARCHAR(255) NOT NULL,
    question_text NVARCHAR(MAX),
    timestamp_seconds INT NOT NULL,
    lesson_id INT,
    FOREIGN KEY (lesson_id) REFERENCES Lessons(lesson_id)
);


-- =========================
-- ENROLLMENTS
-- =========================

CREATE TABLE Enrollments (
    enrollment_id INT PRIMARY KEY IDENTITY(1,1),

    student_id INT NOT NULL,
    course_id INT NOT NULL,

    enrolled_at DATETIME DEFAULT GETDATE(),
    progress_percent FLOAT DEFAULT 0,

    FOREIGN KEY (student_id) REFERENCES Users(user_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

-- =========================
-- ASSIGNMENTS
-- =========================

CREATE TABLE Assignments (
    assignment_id INT PRIMARY KEY IDENTITY(1,1),
    course_id INT NOT NULL,

    assignment_title NVARCHAR(255),
    assignment_description NVARCHAR(MAX),

    due_date DATETIME,
    created_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

-- =========================
-- ASSIGNMENT SUBMISSIONS
-- =========================

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

-- =========================
-- QUIZZES
-- =========================

CREATE TABLE Quizzes (
    quiz_id INT PRIMARY KEY IDENTITY(1,1),

    course_id INT NOT NULL,

    quiz_title NVARCHAR(255),
    duration_minutes INT,

    created_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

-- =========================
-- QUESTIONS
-- =========================

CREATE TABLE Questions (
    question_id INT PRIMARY KEY IDENTITY(1,1),

    quiz_id INT NOT NULL,

    question_content NVARCHAR(MAX),
    correct_answer NVARCHAR(255),
	explanation NVARCHAR(MAX) null,

    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id)
);

-- =========================
-- QUIZ OPTIONS
-- =========================

CREATE TABLE QuestionOptions (
    option_id INT PRIMARY KEY IDENTITY(1,1),

    question_id INT NOT NULL,
    option_content NVARCHAR(255),

    FOREIGN KEY (question_id) REFERENCES Questions(question_id)
);

-- =========================
-- QUIZ ATTEMPTS
-- =========================

CREATE TABLE QuizAttempts (
    attempt_id INT PRIMARY KEY IDENTITY(1,1),

    quiz_id INT NOT NULL,
    student_id INT NOT NULL,

    score FLOAT,
    started_at DATETIME,
    submitted_at DATETIME,

    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

-- =========================
-- PAYMENTS
-- =========================

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

-- =========================
-- REVIEWS
-- =========================

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

-- =========================
-- NOTIFICATIONS
-- =========================

CREATE TABLE Notifications (
    notification_id INT PRIMARY KEY IDENTITY(1,1),

    user_id INT NOT NULL,

    title NVARCHAR(255),
    content NVARCHAR(MAX),

    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- =========================
-- AI CHAT HISTORY
-- =========================

CREATE TABLE AIChatHistory (
    chat_id INT PRIMARY KEY IDENTITY(1,1),
    student_id INT NOT NULL,
    question NVARCHAR(MAX),
    ai_response NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    request_type VARCHAR(50),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

-- =========================
-- SYSTEM REPORTS
-- =========================

CREATE TABLE Reports (
    report_id INT PRIMARY KEY IDENTITY(1,1),

    reporter_id INT NOT NULL,

    report_type NVARCHAR(100),
    report_content NVARCHAR(MAX),

    created_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY (reporter_id) REFERENCES Users(user_id)
);

-- =========================
-- AUDIT LOGS
-- =========================

CREATE TABLE AuditLogs (
    log_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT,
    action_name NVARCHAR(255),
    action_time DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE TestSessions (
    sessions_id INT PRIMARY KEY IDENTITY(1,1),
    quiz_id INT NOT NULL,
    student_id INT NOT NULL,
    started_at DATETIME DEFAULT GETDATE(),
    submitted_at DATETIME NULL,
    remaining_time INT NOT NULL,           -- seconds
    status NVARCHAR(20) DEFAULT 'IN_PROGRESS',
    score FLOAT NULL,
    ip_address NVARCHAR(50),
    user_agent NVARCHAR(255),

    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

CREATE TABLE StudentAnswers (
    answer_id INT PRIMARY KEY IDENTITY(1,1),
    sessions_id INT NOT NULL,
    question_id INT NOT NULL,
    selected_option_id INT NULL,
    answered_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY (sessions_id) REFERENCES TestSessions(sessions_id),
    FOREIGN KEY (question_id) REFERENCES Questions(question_id),
    FOREIGN KEY (selected_option_id) REFERENCES QuestionOptions(option_id)
);

-- =========================================================
-- INSERT DATA
-- =========================================================

INSERT INTO Roles(role_name)
VALUES
('ADMIN'),
('TEACHER'),
('STUDENT');

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

UPDATE Users SET role_name = 'ADMIN'   WHERE role_id = 1;
UPDATE Users SET role_name = 'TEACHER' WHERE role_id = 2;
UPDATE Users SET role_name = 'STUDENT' WHERE role_id = 3;

INSERT INTO Categories(category_name)
VALUES
('Natural Sciences'),
('Social Sciences'),
('Languages'),
('University Preparation');

SET IDENTITY_INSERT [dbo].[chapters] ON;
INSERT [dbo].[chapters] ([chapter_id], [chapter_order], [chapter_title], [course_id]) VALUES
(1, 1, N'Chapter 1: Derivatives & Integrals', 1),
(2, 1, N'Chapter 1: Mechanics', 2),
(3, 1, N'Chapter 1: Foundations', 3),
(14, 3, N'Chapter 3: Hàm số luỹ thừa', 1),
(16, 4, N'Chapter3: Xác xuất thống kê', 1);
SET IDENTITY_INSERT [dbo].[chapters] OFF;

SELECT * FROM Courses


INSERT INTO Subjects(subject_name, category_id)
VALUES
('Mathematics', 1),
('Physics', 1),
('Chemistry', 1),
('Literature', 2),
('English', 3),
('History', 2),
('Geography', 2)

DELETE FROM StudentAnswers;
DELETE FROM TestSessions;
DELETE FROM Questions;
DELETE FROM QuestionOptions;
DELETE FROM Quizzes;
DELETE FROM Lessons
DELETE FROM LearningMaterials
DELETE FROM Enrollments
DELETE FROM Assignments
DELETE FROM AssignmentSubmissions
DELETE FROM Courses;

DBCC CHECKIDENT ('StudentAnswers', RESEED, 0);
DBCC CHECKIDENT ('TestSessions', RESEED, 0);
DBCC CHECKIDENT ('Questions', RESEED, 0);
DBCC CHECKIDENT ('QuestionOptions', RESEED, 0);
DBCC CHECKIDENT ('Lessons', RESEED, 0);
DBCC CHECKIDENT ('Quizzes', RESEED, 0);
DBCC CHECKIDENT ('LearningMaterials', RESEED, 0);
DBCC CHECKIDENT ('Enrollments', RESEED, 0);
DBCC CHECKIDENT ('Assignments', RESEED, 0);
DBCC CHECKIDENT ('AssignmentSubmissions', RESEED, 0);
DBCC CHECKIDENT ('Courses', RESEED, 0);

INSERT INTO Courses
(
    teacher_id,
    subject_id,
    course_title,
    course_description,
    thumbnail_url,
    price,
    is_published,
    status
)
VALUES
(
    2, 1, 'Mastering Mathematics 12',
    'Complete mathematics course for National High School Exam preparation.',
    '/uploads/thumbnails/math-course.jpg',
    599000,
    1,
    'PUBLISHED'
),
(
    3, 2, 'Physics Problem Solving Techniques',
    'Advanced physics lessons and mock exam strategies.',
    '/uploads/thumbnails/physics-course.jpg',
    499000,
    1,
    'PUBLISHED'
),
(
    4, 5, 'English Vocabulary & Grammar',
    'Comprehensive English preparation for university entrance exam.',
    '/uploads/thumbnails/english-course.jpg',
    399000,
    1,
    'PUBLISHED'
),
(
    2, 1, 'Tuyệt đỉnh Casio - Giải nhanh trắc nghiệm Toán',
    'Bí kíp sử dụng máy tính Casio Fx-580VNX & Fx-880BTG cho kỳ thi THPT.',
    '/uploads/thumbnails/casio-course.jpg',
    299000,
    0,
    'PENDING'
);

SET IDENTITY_INSERT [dbo].[Lessons] ON;
INSERT INTO Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, chapter_id, course_id, duration) VALUES
(1, N'Derivative Basics', N'Introduction to derivatives and formulas.', N'https://www.youtube.com/watch?v=hiWjWvba1Kc', N'/uploads/subtitles/math/derivative-basics.vtt', 1, 1, 1, N'11:38'),
(2, N'Applications of Derivatives', N'Optimization and graph analysis.', N'https://www.youtube.com/watch?v=Oa-mVxGS4cw', N'/uploads/subtitles/math/applications-derivatives.vtt', 2, 1, 1, N'22:44'),
(3, N'Integral Fundamentals', N'Basic integration techniques.', N'https://www.youtube.com/watch?v=ndIRu-bUBx0', N'/uploads/subtitles/math/integral-fundamentals.vtt', 3, 1, 1, N'03:42'),
(4, N'Newton Laws of Motion', N'Force and motion concepts.', N'/uploads/videos/physics/newton-laws.mp4', N'/uploads/subtitles/physics/newton-laws.vtt', 1, 2, 2, N'15:30'),
(5, N'Circular Motion', N'Uniform circular motion formulas.', N'/uploads/videos/physics/circular-motion.mp4', N'/uploads/subtitles/physics/circular-motion.vtt', 2, 2, 2, N'20:00'),
(6, N'Grammar Basics', N'English grammar foundation.', N'/uploads/videos/english/grammar-basics.mp4', N'/uploads/subtitles/english/grammar-basics.vtt', 1, 3, 3, N'15:30'),
(7, N'Vocabulary Building', N'Vocabulary improvement methods.', N'/uploads/videos/english/vocabulary-building.mp4', N'/uploads/subtitles/english/vocabulary-building.vtt', 2, 3, 3, N'20:00');
SET IDENTITY_INSERT [dbo].[Lessons] OFF;


INSERT INTO LearningMaterials
(
    lesson_id,
    material_title,
    file_url
)
VALUES
(1, 'Derivative Formula Summary', '/uploads/documents/math/derivative-summary.pdf'),
(2, 'Optimization Exercises', '/uploads/documents/math/optimization.docx'),
(3, 'Integral Practice Sheet', '/uploads/documents/math/integral-practice.pdf'),

(4, 'Newton Laws Summary', '/uploads/documents/physics/newton-summary.pdf'),
(5, 'Circular Motion Exercises', '/uploads/documents/physics/circular-motion.pdf'),

(6, 'Grammar Handbook', '/uploads/documents/english/grammar-handbook.pdf'),
(7, 'Vocabulary Workbook', '/uploads/documents/english/vocabulary-workbook.pdf');


INSERT INTO Enrollments
(
    student_id,
    course_id,
    progress_percent
)
VALUES
(5, 1, 75),
(5, 2, 50),

(6, 1, 90),
(6, 3, 65),

(7, 2, 40),
(7, 3, 85);



INSERT INTO Assignments
(
    course_id,
    assignment_title,
    assignment_description,
    due_date
)
VALUES
(
    1,
    'Derivative Homework',
    'Complete derivative exercises from chapter 1.',
    '2026-06-30'
),
(
    2,
    'Physics Force Assignment',
    'Solve force and motion problems.',
    '2026-07-05'
),
(
    3,
    'English Essay Writing',
    'Write a short essay about education.',
    '2026-07-10'
);



INSERT INTO AssignmentSubmissions
(
    assignment_id,
    student_id,
    submission_text,
    file_url,
    score,
    feedback
)
VALUES
(
    1,
    5,
    'Completed all derivative questions.',
    '/uploads/submissions/math/student5-derivative.pdf',
    8.5,
    'Good understanding, improve presentation.'
),
(
    2,
    6,
    'Physics assignment completed.',
    '/uploads/submissions/physics/student6-force.pdf',
    9.0,
    'Excellent calculations.'
),
(
    3,
    7,
    'Essay submitted successfully.',
    '/uploads/submissions/english/student7-essay.docx',
    8.0,
    'Grammar needs slight improvement.'
);


INSERT INTO Quizzes (course_id, quiz_title, duration_minutes) VALUES
(1, N'Đề thi thử Toán THPT Quốc gia 2026 - Đề 1', 90),
(1, N'Bài tập trắc nghiệm Đạo hàm và Tích phân', 60);

-- VẬT LÝ (Course 2)
DECLARE @QuizLy INT   = (SELECT MAX(quiz_id) - 2 FROM Quizzes)
INSERT INTO Quizzes (course_id, quiz_title, duration_minutes) VALUES
(2, N'Đề thi thử Vật Lý THPT Quốc gia 2026', 50),
(2, N'Bài tập cơ học và Động lực học', 45);

-- TIẾNG ANH (Course 3)
DECLARE @QuizAnh INT  = (SELECT MAX(quiz_id) - 3 FROM Quizzes)
INSERT INTO Quizzes (course_id, quiz_title, duration_minutes) VALUES
(3, N'Đề thi thử Tiếng Anh THPT - Reading & Grammar', 50),
(3, N'Bài kiểm tra từ vựng và cấu trúc câu', 40);

--Toan

-- ====================== 30 CÂU HỎI TOÁN ======================
DECLARE @QuizToan INT = (SELECT TOP 1 quiz_id FROM Quizzes WHERE course_id = 1 ORDER BY quiz_id DESC);
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES
(@QuizToan, N'Giá trị lim (x→0) sin(3x)/x là bao nhiêu?', '3', N'Áp dụng lim sin(kx)/x = k'),
(@QuizToan, N'Đạo hàm của y = x^3 + 3x^2 - 5x + 7 là?', '3x^2 + 6x - 5', N'Đạo hàm từng số'),
(@QuizToan, N'Phương trình bậc hai ax² + bx + c = 0 có nghiệm kép khi?', 'Δ = 0', N'Δ = b² - 4ac = 0'),
(@QuizToan, N'Diện tích hình tròn bán kính r là?', 'πr²', NULL),
(@QuizToan, N'log₂(8) bằng bao nhiêu?', '3', NULL),
(@QuizToan, N'Đạo hàm của y = sin(2x) là?', '2cos(2x)', NULL),
(@QuizToan, N'Giải phương trình x² - 5x + 6 = 0?', 'x=2 hoặc x=3', NULL),
(@QuizToan, N'Tích phân của x² dx là?', '(x^3)/3 + C', NULL),
(@QuizToan, N'Đường thẳng y = 2x + 3 cắt trục hoành tại?', '(-3/2, 0)', NULL),
(@QuizToan, N'Số nghiệm của sin(x) = 0 trong [0, 2π] là?', '3', NULL),
(@QuizToan, N'Đạo hàm bậc hai của y = x^4 là?', '12x²', NULL),
(@QuizToan, N'Giá trị C(5,2) là?', '10', NULL),
(@QuizToan, N'Phương trình tiếp tuyến của y = x² tại x=2 là?', 'y = 4x - 4', NULL),
(@QuizToan, N'Tập nghiệm bất phương trình x² - 4x + 3 > 0 là?', 'x < 1 hoặc x > 3', NULL),
(@QuizToan, N'log₃(27) bằng bao nhiêu?', '3', NULL),
(@QuizToan, N'Diện tích hình chữ nhật dài 5, rộng 3 là?', '15', NULL),
(@QuizToan, N'Đường chéo hình vuông cạnh a là?', 'a√2', NULL),
(@QuizToan, N'Giá trị nhỏ nhất của y = x² - 4x + 5 là?', '1', NULL),
(@QuizToan, N'cos(60°) bằng bao nhiêu?', '0.5', NULL),
(@QuizToan, N'Thể tích hình cầu bán kính r là?', '(4/3)πr³', NULL),
(@QuizToan, N'Số nguyên tố nhỏ nhất lớn hơn 20 là?', '23', NULL),
(@QuizToan, N'Giải |x-2| < 3?', '-1 < x < 5', NULL),
(@QuizToan, N'Đạo hàm của e^(2x) là?', '2e^(2x)', NULL),
(@QuizToan, N'Tổng nghiệm phương trình x³ - 6x² + 11x - 6 = 0 là?', '6', NULL),
(@QuizToan, N'Diện tích tam giác đáy 6, cao 4 là?', '12', NULL),
(@QuizToan, N'Giá trị 2^10 là?', '1024', NULL),
(@QuizToan, N'Phương trình x² + 1 = 0 có nghiệm thực không?', 'Không', NULL),
(@QuizToan, N'Phương trình parabol y = ax² + bx + c qua (1,3) và (2,7). Tìm a+b+c?', '5', NULL),
(@QuizToan, N'Tích hai nghiệm của x² - 7x + 12 = 0 là?', '12', NULL),
(@QuizToan, N'Giá trị nhỏ nhất của hàm y = x² - 6x + 10 là?', '1', NULL);

-- ====================== 30 CÂU HỎI LÝ ======================
-- DECLARE @QuizLy INT   = (SELECT MAX(quiz_id) - 2 FROM Quizzes)
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES
(@QuizLy, N'Đơn vị SI của lực là gì?', 'Newton', NULL),
(@QuizLy, N'Công thức tính gia tốc là?', 'a = F/m', NULL);

-- ====================== 30 CÂU HỎI ANH ======================
-- DECLARE @QuizAnh INT  = (SELECT MAX(quiz_id) - 3 FROM Quizzes)
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation) VALUES
(@QuizAnh, N'She _____ to school every day.', 'goes', N'Ngôi thứ 3 số ít phải thêm "s"'),
(@QuizAnh, N'I have lived in Hanoi _____ 2015.', 'since', NULL),
(@QuizAnh, N'This is the book _____ I borrowed from the library.', 'that', NULL);

-- Thêm options theo từng câu một cách an toàn

-- ==================== THÊM OPTIONS ====================
-- Câu 1
INSERT INTO QuestionOptions VALUES
(1,N'1'),(1,N'2'),(1,N'3'),(1,N'6');

-- Câu 2
INSERT INTO QuestionOptions VALUES
(2,N'3x²+5x'),(2,N'6x²-5'),(2,N'3x²+6x-5'),(2,N'x²+6x-5');

-- Câu 3
INSERT INTO QuestionOptions VALUES
(3,N'Δ > 0'),(3,N'Δ = 0'),(3,N'Δ < 0'),(3,N'a = 0');

-- Câu 4
INSERT INTO QuestionOptions VALUES
(4,N'2πr'),(4,N'πr²'),(4,N'πd'),(4,N'r²');

-- Câu 5
INSERT INTO QuestionOptions VALUES
(5,N'2'),(5,N'4'),(5,N'3'),(5,N'8');

-- Câu 6
INSERT INTO QuestionOptions VALUES
(6,N'2sin(2x)'),(6,N'cos(2x)'),(6,N'2cos(2x)'),(6,N'sin(2x)');

-- Câu 7
INSERT INTO QuestionOptions VALUES
(7,N'x=2 hoặc x=3'),(7,N'x=1 hoặc x=6'),(7,N'x=2'),(7,N'x=3');

-- Câu 8
INSERT INTO QuestionOptions VALUES
(8,N'x²+C'),(8,N'(x^3)/3 + C'),(8,N'3x²+C'),(8,N'x³+C');

-- Câu 9
INSERT INTO QuestionOptions VALUES
(9,N'(3/2,0)'),(9,N'(-3/2,0)'),(9,N'(0,3)'),(9,N'(2,3)');

-- Câu 10
INSERT INTO QuestionOptions VALUES
(10,N'2'),(10,N'4'),(10,N'3'),(10,N'1');

-- Câu 11
INSERT INTO QuestionOptions VALUES
(11,N'12x²'),(11,N'4x³'),(11,N'6x²'),(11,N'24x');

-- Câu 12
INSERT INTO QuestionOptions VALUES
(12,N'5'),(12,N'20'),(12,N'10'),(12,N'15');

-- Câu 13
INSERT INTO QuestionOptions VALUES
(13,N'y=2x'),(13,N'y=4x-4'),(13,N'y=4x+4'),(13,N'y=x²');

-- Câu 14
INSERT INTO QuestionOptions VALUES
(14,N'x<1 hoặc x>3'),(14,N'1<x<3'),(14,N'x>1'),(14,N'x<3');

-- Câu 15
INSERT INTO QuestionOptions VALUES
(15,N'9'),(15,N'2'),(15,N'3'),(15,N'1');

-- Câu 16
INSERT INTO QuestionOptions VALUES
(16,N'8'),(16,N'15'),(16,N'10'),(16,N'12');

-- Câu 17
INSERT INTO QuestionOptions VALUES
(17,N'a√2'),(17,N'2a'),(17,N'a²'),(17,N'a/2');

-- Câu 18
INSERT INTO QuestionOptions VALUES
(18,N'0'),(18,N'1'),(18,N'2'),(18,N'3');

-- Câu 19
INSERT INTO QuestionOptions VALUES
(19,N'0.5'),(19,N'1'),(19,N'√3/2'),(19,N'0');

-- Câu 20
INSERT INTO QuestionOptions VALUES
(20,N'πr³'),(20,N'(4/3)πr³'),(20,N'4πr²'),(20,N'r³');

-- Câu 21
INSERT INTO QuestionOptions VALUES
(21,N'21'),(21,N'23'),(21,N'25'),(21,N'29');

-- Câu 22
INSERT INTO QuestionOptions VALUES
(22,N'-1<x<5'),(22,N'x>5'),(22,N'x<2'),(22,N'0<x<3');

-- Câu 23
INSERT INTO QuestionOptions VALUES
(23,N'e^(2x)'),(23,N'2e^(2x)'),(23,N'2x·e'),(23,N'e^x');

-- Câu 24
INSERT INTO QuestionOptions VALUES
(24,N'3'),(24,N'6'),(24,N'11'),(24,N'18');

-- Câu 25
INSERT INTO QuestionOptions VALUES
(25,N'10'),(25,N'24'),(25,N'12'),(25,N'20');

-- Câu 26
INSERT INTO QuestionOptions VALUES
(26,N'512'),(26,N'1000'),(26,N'1024'),(26,N'2048');

-- Câu 27
INSERT INTO QuestionOptions VALUES
(27,N'Có 2 nghiệm'),(27,N'Có 1 nghiệm'),(27,N'Không'),(27,N'x=1');

-- Câu 28
INSERT INTO QuestionOptions VALUES
(28,N'5'),(28,N'3'),(28,N'7'),(28,N'10');

-- Câu 29
INSERT INTO QuestionOptions VALUES
(29,N'7'),(29,N'12'),(29,N'5'),(29,N'19');

-- Câu 30
INSERT INTO QuestionOptions VALUES
(30,N'0'),(30,N'1'),(30,N'2'),(30,N'4');

-- ====================== OPTIONS CHI TIẾT ======================


-- Thêm lựa chọn cho từng câu


INSERT INTO QuizAttempts
(
    quiz_id,
    student_id,
    score,
    started_at,
    submitted_at
)
VALUES
(1, 5, 8.5, GETDATE(), GETDATE()),
(2, 6, 9.0, GETDATE(), GETDATE()),
(3, 7, 7.5, GETDATE(), GETDATE());

-- Thêm dữ liệu mẫu cho Notifications
INSERT INTO Notifications
    (title, content, target_role, created_by)
VALUES
    ('📢 Cập nhật tính năng Lộ trình AI mới',
     'Hệ thống vừa nâng cấp thuật toán phân tích năng lực. Truy cập tab Lộ trình AI để xem gợi ý học tập mới nhất dành cho bạn!',
     'ALL', 1),

    ('⏰ Nhắc nhở lịch học',
     'Bạn có lịch Luyện đề Toán số 1 vào lúc 19:00 tối nay. Nhớ chuẩn bị giấy nháp và máy tính Casio nhé!',
     'STUDENT', 1),

    ('✅ Kết quả chấm bài',
     'Giáo viên Nguyễn Minh Quân đã chấm xong bài tập "Derivative Homework" của bạn. Điểm: 8.5/10.',
     'STUDENT', 2),

    ('❌ Khóa học bị từ chối xuất bản',
     'Admin đã từ chối xuất bản khóa học của bạn. Lý do: Thiếu video giới thiệu chương 2. Vui lòng kiểm tra và chỉnh sửa lại.',
     'TEACHER', 1),

    ('🎉 Khóa học mới được duyệt',
     'Khóa học "Tuyệt đỉnh Casio" của bạn đã được Admin duyệt và xuất bản thành công!',
     'TEACHER', 1);

--ALTER TABLE

-- Thêm bảng Notifications cho Admin gửi thông báo

-- Cập nhật một số khóa học thành PENDING để test
UPDATE Courses SET status = 'PENDING' WHERE is_published = 0;

--------------------------------------------------------------
SELECT * FROM AIChatHistory;
SELECT * FROM AuditLogs;
SELECT * FROM Courses;
SELECT * FROM Users;
DELETE FROM Users
where user_id = 10

DELETE FROM QuizAttempts;
DBCC CHECKIDENT ('QuizAttempts', RESEED, 0);

SELECT local_tcp_port
FROM sys.dm_exec_connections
WHERE session_id = @@SPID;

DELETE FROM StudentAnswers;
DELETE FROM TestSessions;
DELETE FROM QuestionOptions;
DELETE FROM Questions;
DELETE FROM Quizzes;

DBCC CHECKIDENT ('StudentAnswers', RESEED, 0);
DBCC CHECKIDENT ('TestSessions', RESEED, 0);
DBCC CHECKIDENT ('QuestionOptions', RESEED, 0);
DBCC CHECKIDENT ('Questions', RESEED, 0);
DBCC CHECKIDENT ('Quizzes', RESEED, 0);
PRINT 'Đã xóa sạch data quiz!';