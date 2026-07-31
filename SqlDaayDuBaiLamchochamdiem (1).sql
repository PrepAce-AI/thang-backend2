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
	education nvarchar(max) null,
	experience nvarchar(max) null,
	teacherRequestStatus nvarchar (50) null,
	lock_reason nvarchar(max) null,
	teacher_request_status varchar(255) null,

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

CREATE TABLE SystemConfig(
    config_key varchar(100) NOT NULL PRIMARY KEY,
    config_value nvarchar(max) NOT NULL
);

CREATE TABLE Subjects (
    subject_id INT PRIMARY KEY IDENTITY(1,1),
    subject_name NVARCHAR(100) NOT NULL,
    category_id INT,
	is_hidden bit not null default (0),
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
	description nvarchar(max) null,
	note nvarchar(max) null,
	category_id int null,
    FOREIGN KEY (teacher_id) REFERENCES Users(user_id),
    FOREIGN KEY (subject_id) REFERENCES Subjects(subject_id),
	FOREIGN KEY (category_id) REFERENCES Categories(category_id)
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
	content NVARCHAR(MAX) NULL,
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

CREATE TABLE StudentProgress(
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    is_completed bit NOT NULL,
    last_accessed datetime2(6) NULL,
    score float NULL,
    lesson_id int NOT NULL,
    user_id int NOT NULL,
    FOREIGN KEY(lesson_id) REFERENCES Lessons (lesson_id),
    FOREIGN KEY(user_id) REFERENCES Users (user_id)
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

CREATE TABLE FlashQuizzes(
    flash_quiz_id INT IDENTITY PRIMARY KEY,
    student_id INT NOT NULL,
    chapter_id INT NOT NULL,
    total_questions INT DEFAULT 5,
    correct_answers INT DEFAULT 0,
    score DECIMAL(5,2),
    status VARCHAR(20) DEFAULT 'GENERATED',
    created_at DATETIME DEFAULT GETDATE(),
    completed_at DATETIME NULL,
	ai_model NVARCHAR(100) NULL,
    FOREIGN KEY(student_id) REFERENCES Users(user_id),
    FOREIGN KEY(chapter_id) REFERENCES Chapters(chapter_id)
);

CREATE TABLE FlashQuizQuestions(
    question_id INT IDENTITY PRIMARY KEY,
    flash_quiz_id INT,
    question NVARCHAR(MAX),
    option_a NVARCHAR(500),
    option_b NVARCHAR(500),
    option_c NVARCHAR(500),
    option_d NVARCHAR(500),
    correct_answer CHAR(1),
    explanation NVARCHAR(MAX),
    FOREIGN KEY(flash_quiz_id)
        REFERENCES FlashQuizzes(flash_quiz_id)
);

CREATE TABLE FlashQuizAnswers(
    answer_id INT IDENTITY PRIMARY KEY,
    flash_quiz_id INT,
    question_id INT,
    selected_answer CHAR(1),
    is_correct BIT,
    FOREIGN KEY(flash_quiz_id)
        REFERENCES FlashQuizzes(flash_quiz_id),
    FOREIGN KEY(question_id)
        REFERENCES FlashQuizQuestions(question_id)
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
	question_type varchar(50) null,
    FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id)
);

CREATE TABLE QuestionOptions (
    option_id INT IDENTITY(1,1) PRIMARY KEY,
    question_id INT NOT NULL,
    option_content NVARCHAR(MAX) NOT NULL,
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
    correct_count int null,
	total_questions int null
	
	FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id),
    FOREIGN KEY (student_id) REFERENCES Users(user_id)
);

CREATE TABLE test_sessions(
    sessions_id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    ip_address varchar(255) NULL,
    remaining_time int NULL,
    score real NULL,
    started_at datetime2(6) NULL,
    status varchar(255) NULL,
    submitted_at datetime2(6) NULL,
    user_agent varchar(255) NULL,
    quiz_id int NOT NULL,
    user_id int NOT NULL,

	FOREIGN KEY (user_id) REFERENCES Users(user_id),
	FOREIGN KEY (quiz_id) REFERENCES Quizzes(quiz_id)
);

CREATE TABLE StudentAnswers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    selected_option_id INT NULL,
    answered_at DATETIME DEFAULT GETDATE(),
	essay_answer nvarchar(max) null,
	score float null,
	teacher_comment nvarchar(max) null,

    FOREIGN KEY (attempt_id) REFERENCES QuizAttempts(attempt_id),
	FOREIGN KEY (attempt_id) REFERENCES test_sessions(sessions_id),
    FOREIGN KEY (question_id) REFERENCES Questions(question_id),
    FOREIGN KEY (selected_option_id) REFERENCES QuestionOptions(option_id)
);

CREATE TABLE PracticeAnswers(
    practice_answer_id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    is_correct bit NULL,
    question_order int NOT NULL,
    selected_option_id int NULL,
    attempt_id int NOT NULL,
    question_id int NOT NULL,
    FOREIGN KEY(attempt_id) REFERENCES QuizAttempts(attempt_id),
    FOREIGN KEY(question_id) REFERENCES Questions(question_id)
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
	created_at DATETIME2(6) NULL,
	user_id int not null,
    FOREIGN KEY (student_id) REFERENCES Users(user_id),
    FOREIGN KEY (course_id) REFERENCES Courses(course_id),
	FOREIGN KEY (user_id) REFERENCES Users(user_id)
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

CREATE TABLE AIChapterSummary (
    summary_id INT IDENTITY(1,1) PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    chapter_id INT NOT NULL,
    ai_model NVARCHAR(50),
    summary_content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_AISummary_User
        FOREIGN KEY(student_id) REFERENCES Users(user_id),
    CONSTRAINT FK_AISummary_Course
        FOREIGN KEY(course_id) REFERENCES Courses(course_id),
    CONSTRAINT FK_AISummary_Chapter
        FOREIGN KEY(chapter_id) REFERENCES Chapters(chapter_id)
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

CREATE TABLE StudySchedules(
	schedule_id INT PRIMARY KEY IDENTITY(1,1) NOT NULL,
	user_id int not null,
	title nvarchar(255) not null,
	schedule_date date NOT NULL,
    schedule_time varchar(10) NOT NULL,
    schedule_type varchar(20) NOT NULL DEFAULT ('math'),
    created_at datetime NOT NULL DEFAULT (getdate()),
    reminder_sent bit NOT NULL DEFAULT ((0)),

	FOREIGN KEY (user_id) REFERENCES Users(user_id),
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

CREATE TABLE ViolationReports(
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    reporter_id int NULL,
    reported_target nvarchar(255) NULL,
    reason nvarchar(1000) NULL,
    status varchar(255) NULL,
    admin_note nvarchar(1000) NULL,
    created_at datetime NOT NULL DEFAULT (getdate())
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

INSERT INTO SystemConfig (config_key, config_value) VALUES 
('banner_title', N'Bứt phá điểm số cùng PrepAce AI'),
('banner_subtitle', N'Hệ thống học tập thông minh sử dụng AI để phân tích năng lực, xây dựng lộ trình cá nhân và tối ưu kết quả kỳ thi THPT Quốc Gia.'),
('banner_btn_text', N'Bắt đầu ngay');

SET IDENTITY_INSERT [dbo].[Users] ON 

INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (1, N'System Administrator', N'admin@learnifyfuture.com', N'123456', N'0901111111', N'admin.jpg', 1, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, NULL, NULL, N'ADMIN', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (2, N'Thầy Quang', N'gv.toan@prepace.vn', N'123456', N'0902222222', N'teacher1.jpg', 2, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, N'FPT University', N'Giáo viên chuyên luyện thi môn Toán Học THPT Quốc gia', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (3, N'Cô Hương', N'gv.van@prepace.vn', N'123456', N'0903333333', N'teacher2.jpg', 2, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, N'Hanoi University', N'Giáo viên chuyên luyện thi môn Ngữ Văn THPT Quốc gia', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (4, N'Cô Mai', N'gv.anh@prepace.vn', N'123456', N'0904444444', N'teacher3.jpg', 2, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, N'University of Languages', N'Giáo viên chuyên luyện thi môn Tiếng Anh THPT Quốc gia', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (5, N'Pham Duc Anh', N'student1@gmail.com', N'123456', N'0905555555', N'student1.jpg', 3, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, N'THPT Chu Van An', N'Student interested in science', N'STUDENT', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (6, N'Vo Minh Tri', N'student2@gmail.com', N'123456', N'0906666666', N'student2.jpg', 3, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, N'THPT Le Quy Don', N'Good at math and physics', N'STUDENT', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (7, N'Nguyen Thanh Dat', N'student3@gmail.com', N'123456', N'0907777777', N'student3.jpg', 3, N'ACTIVE', CAST(N'2026-07-13T22:30:19.047' AS DateTime), NULL, NULL, N'THPT Nguyen Hue', N'Preparing for university exam', N'STUDENT', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (8, N'Toàn Trần', N'ahhi798567@gmail.com', N'$2a$12$74yFmrfBrCpL3vSyw.bLCejjZ9YVU8qEn5tVIMptAiMA7o7Z7h0Z2', NULL, N'https://lh3.googleusercontent.com/a/ACg8ocJVZWiHm7RfjG-QwsSREq5UKCaVzWTiL2h3ShVDxi3l8CDCNA=s96-c', 3, N'ACTIVE', CAST(N'2026-07-14T01:15:06.980' AS DateTime), NULL, NULL, NULL, NULL, N'STUDENT', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (9, N'Cô Hồng', N'gv.vatly@prepace.vn', N'123456', NULL, NULL, 2, N'ACTIVE', CAST(N'2026-07-15T01:57:43.223' AS DateTime), NULL, NULL, NULL, N'Giáo viên chuyên Vật Lý', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (10, N'Thầy Minh', N'gv.lichsu@prepace.vn', N'123456', NULL, NULL, 2, N'ACTIVE', CAST(N'2026-07-15T01:57:43.223' AS DateTime), NULL, NULL, NULL, N'Giáo viên chuyên Lịch Sử', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (11, N'Cô Lan', N'gv.diali@prepace.vn', N'123456', NULL, NULL, 2, N'ACTIVE', CAST(N'2026-07-15T01:57:43.223' AS DateTime), NULL, NULL, NULL, N'Giáo viên chuyên Địa Lí', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
INSERT Users (user_id, full_name, email, password_hash, phone, avatar_url, role_id, account_status, created_at, verification_code, verification_expiry, school, bio, role_name, education, experience, teacherRequestStatus, lock_reason, teacher_request_status) VALUES (12, N'Thầy Phong', N'gv.hoahoc@prepace.vn', N'123456', NULL, NULL, 2, N'ACTIVE', CAST(N'2026-07-15T01:57:43.223' AS DateTime), NULL, NULL, NULL, N'Giáo viên chuyên Hóa Học', N'TEACHER', NULL, NULL, NULL, NULL, NULL)
SET IDENTITY_INSERT [dbo].[Users] OFF
UPDATE Users SET role_name = 'ADMIN' WHERE role_id = 1;
UPDATE Users SET role_name = 'TEACHER' WHERE role_id = 2;
UPDATE Users SET role_name = 'STUDENT' WHERE role_id = 3;


SELECT * FROM Users
INSERT INTO Categories(category_name) VALUES ('Natural Sciences'), ('Social Sciences'), ('Languages'), ('University Preparation');

SET IDENTITY_INSERT Subjects ON 

INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (1, N'Mathematics', 1, 0)
INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (2, N'Physics', 1, 0)
INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (3, N'Chemistry', 1, 0)
INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (4, N'Literature', 2, 0)
INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (5, N'English', 3, 0)
INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (7, N'Geography', 2, 0)
INSERT Subjects (subject_id, subject_name, category_id, is_hidden) VALUES (6, N'History', 2, 0)
SET IDENTITY_INSERT Subjects OFF

SET IDENTITY_INSERT Courses ON 
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (1, 2, 1, N'Toán Học 12', N'Khóa học Toán Học 12 toàn diện, bao gồm Đại số, Giải tích và Hình học. Trang bị kiến thức nền tảng vững chắc cùng các phương pháp giải siêu tốc, giúp học sinh tự tin chinh phục điểm 9+ trong kỳ thi THPT Quốc gia.', N'/uploads/thumbnails/math-course.jpg', CAST(599000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-11T21:29:40.457' AS DateTime), N'PUBLISHED', NULL, NULL, NULL, NULL, NULL)
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (2, 3, 4, N'Ngữ Văn 12', N'Khóa học Ngữ Văn 12 chuyên sâu, hướng dẫn kỹ năng phân tích tác phẩm văn học, nghị luận xã hội và rèn luyện tư duy phản biện. Cung cấp các "công thức" lập dàn ý chi tiết để viết bài đạt điểm cao.', N'/uploads/thumbnails/van.jpg', CAST(499000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-11T21:29:40.457' AS DateTime), N'PUBLISHED', NULL, NULL, NULL, NULL, NULL)
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (3, 4, 5, N'Tiếng Anh 12', N'Lộ trình Tiếng Anh 12 bài bản từ mất gốc đến nâng cao. Tổng hợp toàn bộ ngữ pháp trọng tâm, từ vựng theo chủ đề và kỹ năng đọc hiểu - điền từ, sát với cấu trúc đề thi Đại học thực tế.', N'/uploads/thumbnails/english-course.jpg', CAST(399000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-11T21:29:40.457' AS DateTime), N'PUBLISHED', NULL, NULL, NULL, NULL, NULL)
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (7, 9, 2, N'Vật Lý 12', N'Chinh phục Vật Lý 12 với hệ thống bài giảng trực quan, dễ hiểu. Phân loại từng dạng bài tập từ cơ bản đến vận dụng cao (Dao động cơ, Sóng cơ, Điện xoay chiều...). Đập tan nỗi sợ môn Lý!', N'/uploads/thumbnails/vatli.jpg', CAST(499000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-14T14:17:58.963' AS DateTime), N'DRAFT', NULL, NULL, N'', NULL, 1)
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (8, 10, 6, N'Lịch Sử', N'Biến Lịch Sử thành môn học hấp dẫn bằng phương pháp sơ đồ tư duy (Mindmap) và chuỗi liên kết sự kiện. Tổng ôn trọng tâm Lịch Sử Việt Nam và Thế giới, nắm trọn điểm số dễ dàng.', N'/uploads/thumbnails/su.jpg', CAST(350000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-14T14:29:47.707' AS DateTime), N'DRAFT', NULL, NULL, N'', NULL, 1)
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (9, 11, 7, N'Địa Lí 12', N'Nắm vững kiến thức Địa Lí 12 qua kỹ năng phân tích số liệu, biểu đồ và khai thác Atlat Địa Lí Việt Nam. Lộ trình học thông minh giúp ghi nhớ nhanh, hiểu sâu bản chất.', N'/uploads/thumbnails/dia.jpg', CAST(350000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-14T15:07:31.687' AS DateTime), N'DRAFT', NULL, NULL, N'', NULL, 1)
INSERT Courses (course_id, teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at, status, review_note, reviewed_at, description, note, category_id) VALUES (10, 12, 3, N'Hoá Học 12', N'Giải mã Hóa Học 12 với các mẹo ghi nhớ phản ứng và phương pháp giải bài tập hóa siêu tốc (bảo toàn khối lượng, bảo toàn electron...). Khôi phục nền tảng, bứt phá điểm thi THPT Quốc gia.', N'/uploads/thumbnails/hoa.jpg', CAST(499000.00 AS Decimal(10, 2)), 1, CAST(N'2026-07-14T15:23:36.123' AS DateTime), N'DRAFT', NULL, NULL, N'', NULL, 1)

SET IDENTITY_INSERT Courses OFF

SET IDENTITY_INSERT [dbo].[CourseReviews] ON 

INSERT CourseReviews (review_id, student_id, course_id, rating, comment, reviewed_at, created_at, user_id) VALUES (1, 8, 1, 5, N'.', CAST(N'2026-07-14T01:28:34.913' AS DateTime), CAST(N'2026-07-14T01:28:34.9070000' AS DateTime2), 8)
INSERT CourseReviews (review_id, student_id, course_id, rating, comment, reviewed_at, created_at, user_id) VALUES (2, 8, 1, 1, N'.', CAST(N'2026-07-14T13:05:36.840' AS DateTime), CAST(N'2026-07-14T13:05:36.8330000' AS DateTime2), 8)
SET IDENTITY_INSERT [dbo].[CourseReviews] OFF
-- ===========================================================================
-- 3. INSERT CHAPTERS & LESSONS
-- ===========================================================================

SET IDENTITY_INSERT Chapters ON;
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (17, 1, N'CHƯƠNG I. ỨNG DỤNG ĐẠO HÀM ĐỂ KHẢO SÁT VÀ VẼ ĐỒ THỊ HÀM SỐ', 1, CAST(N'2026-07-12T00:34:11.830' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (18, 2, N'CHƯƠNG II. VECTƠ VÀ HỆ TRỤC TOẠ ĐỘ TRONG KHÔNG GIAN.', 1, CAST(N'2026-07-12T00:54:34.513' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (19, 1, N'Phân Tích Tác Phẩm', 2, CAST(N'2026-07-12T18:53:46.457' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (20, 1, N'Unit 1: Life stories we admire', 3, CAST(N'2026-07-12T19:00:58.553' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (21, 2, N'Unit 2: A multicultural world', 3, CAST(N'2026-07-12T19:03:38.020' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (22, 1, N'Chương 1. Vật lí nhiệt', 7, CAST(N'2026-07-14T14:18:43.110' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (23, 2, N'Chương 2. Khí lí tưởng', 7, CAST(N'2026-07-14T14:18:56.910' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (24, 1, N'Chủ đề 1. THẾ GIỚI TRONG VÀ SAU CHIẾN TRANH LẠNH', 8, CAST(N'2026-07-14T14:31:21.713' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (25, 2, N'Chủ đề 2. ASEAN: NHỮNG CHẶNG ĐƯỜNG LỊCH SỬ', 8, CAST(N'2026-07-14T14:51:25.200' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (26, 1, N'PHẦN 1. ĐỊA LÍ TỰ NHIÊN', 9, CAST(N'2026-07-14T15:09:17.943' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (27, 2, N'PHẦN 2. ĐỊA LÍ DÂN CƯ', 9, CAST(N'2026-07-14T15:13:40.220' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (28, 1, N'Chương 1. Ester – Lipid', 10, CAST(N'2026-07-14T15:25:20.200' AS DateTime))
INSERT Chapters (chapter_id, chapter_order, chapter_title, course_id, created_at) VALUES (29, 2, N'Chương 2. Carbohydrate', 10, CAST(N'2026-07-14T15:30:30.197' AS DateTime))
SET IDENTITY_INSERT Chapters OFF;

SET IDENTITY_INSERT Lessons ON 
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (14, N' Bài 1 - Tính đơn điệu và cực trị của hàm số - Tiết 1', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783855379/videos/ea767ddf-c07f-4f09-92cb-fc27ce521e1c.mp4', NULL, 1, CAST(N'2026-07-12T18:23:01.080' AS DateTime), 17, NULL, N'35:33', 1)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (15, N'Bài 1 - Tính đơn điệu và cực trị của hàm số - Tiết 2', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783855488/videos/22b1a857-8d30-494a-ab97-7fdb5689bd3a.mp4', NULL, 2, CAST(N'2026-07-12T18:24:50.420' AS DateTime), 17, NULL, N'31:24', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (16, N'Bài 6 - Vectơ trong không gian - Tiết 1', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783856454/videos/6a3956c0-e030-4ca7-b54e-69da6ba03e86.mp4', NULL, 1, CAST(N'2026-07-12T18:40:55.970' AS DateTime), 18, NULL, N'22:56', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (17, N'Xuân Tóc Đỏ cứu quốc', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783857353/videos/f86d21cd-1fea-4d04-b7dc-687926dba685.mp4', NULL, 1, CAST(N'2026-07-12T18:55:53.797' AS DateTime), 19, NULL, N'25:06', 1)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (18, N' Nỗi buồn chiến tranh', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783857436/videos/b3380ea4-c483-4dd1-a8e8-2782652a4c5a.mp4', NULL, 2, CAST(N'2026-07-12T18:57:16.963' AS DateTime), 19, NULL, N'28:31', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (19, N'Getting started', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783857681/videos/0b1be488-3f8c-4ea9-8861-21d2bbaa09cd.mp4', NULL, 1, CAST(N'2026-07-12T19:01:21.873' AS DateTime), 20, NULL, N'15:54', 1)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (20, N'Language', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783857773/videos/74ed9ea3-65f1-4daf-a54a-c022e119c000.mp4', NULL, 2, CAST(N'2026-07-12T19:02:53.110' AS DateTime), 20, NULL, N'20:22', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (21, N'Getting started', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783858243/videos/c6a9d48b-bc98-4f93-ab62-f4142240a9a5.mp4', NULL, 1, CAST(N'2026-07-12T19:10:44.530' AS DateTime), 21, NULL, N'17:58', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (22, N'Language', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1783858592/videos/23140843-2f15-467f-8d06-56208ad5838f.mp4', NULL, 2, CAST(N'2026-07-12T19:16:34.230' AS DateTime), 21, NULL, N'20:35', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (23, N'Bài 1. Cấu trúc của chất. Sự chuyển thể', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784013807/videos/1c1b6a3d-2bba-4ba6-ab5e-4aa4104f32ab.mp4', NULL, 1, CAST(N'2026-07-14T14:23:28.480' AS DateTime), 22, NULL, N'22:36', 1)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (24, N'Bài 2. Nội năng. Định luật I của nhiệt động lực học', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784013876/videos/fa5656e8-608c-4df6-ba46-2cda3f560339.mp4', NULL, 2, CAST(N'2026-07-14T14:24:37.000' AS DateTime), 22, NULL, N'28:17', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (25, N'Bài 8. Mô hình động học phân tử chất khí', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784013984/videos/6db16102-c9bb-4c8c-b86c-e86fc53a107e.mp4', NULL, 1, CAST(N'2026-07-14T14:26:25.780' AS DateTime), 23, NULL, N'13:47', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (26, N'Bài 9: Định luật Boyle', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784014123/videos/b3901a5f-b02b-4d50-b1a7-9638397eee74.mp4', NULL, 2, CAST(N'2026-07-14T14:28:44.323' AS DateTime), 23, NULL, N'18:01', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (27, N'Bài 1. Liên hợp quốc', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784015432/videos/6b3b215e-b852-4be9-bd48-88676335e70d.mp4', NULL, 1, CAST(N'2026-07-14T14:50:33.403' AS DateTime), 24, NULL, N'12:23', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (28, N'Bài 2. Trật tự thế giới trong Chiến tranh lạnh', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784015468/videos/d07b1092-767c-4212-be14-620243cceb27.mp4', NULL, 2, CAST(N'2026-07-14T14:51:08.863' AS DateTime), 24, NULL, N'13:54', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (29, N'Bài 4: Sự ra đời và phát triển của Hiệp hội các quốc gia Đông Nam Á', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784016323/videos/48d4a0ae-8639-4b02-974f-6c38bb54e3d8.mp4', NULL, 1, CAST(N'2026-07-14T15:05:23.980' AS DateTime), 25, NULL, N'10:41', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (30, N'Bài 5. Cộng đồng ASEAN: Từ ý tưởng đến hiện thực', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784016390/videos/713ba5ac-9fd3-475c-8fb2-c2f8a38ecbdb.mp4', NULL, 2, CAST(N'2026-07-14T15:06:31.600' AS DateTime), 25, NULL, N'13:17', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (31, N'Bài 1: Vị trí địa lí và phạm vi lãnh thổ', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784016737/videos/d521fbf9-d799-43a6-9bd8-a1b95cd2408e.mp4', NULL, 1, CAST(N'2026-07-14T15:12:18.987' AS DateTime), 26, NULL, N'48:43', 1)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (32, N'Bài 2: Thiên nhiên nhiệt đới ẩm gió mùa', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784016780/videos/7208191f-8bc3-4c5c-94dd-ed3840571705.mp4', NULL, 2, CAST(N'2026-07-14T15:13:01.600' AS DateTime), 26, NULL, N'52:59', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (33, N'Bài 6: Dân số Việt Nam', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784017096/videos/ce21e834-327b-4caf-b6f8-277a91ef9749.mp4', NULL, 1, CAST(N'2026-07-14T15:18:17.210' AS DateTime), 27, NULL, N'36:10', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (34, N'Bài 7: Lao động và việc làm', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784017334/videos/d7652977-2a21-4565-9967-f5f11ae85cea.mp4', NULL, 2, CAST(N'2026-07-14T15:22:15.833' AS DateTime), 27, NULL, N'37:31', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (35, N'Bài 1. Ester – Lipid', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784017691/videos/41b1dfd0-50df-4c95-a170-d810836fc13d.mp4', NULL, 1, CAST(N'2026-07-14T15:28:13.137' AS DateTime), 28, NULL, N'45:36', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (36, N'Bài 2: Xà phòng và chất giặt rửa', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784017792/videos/6a59435f-6788-49c1-9669-0891ef8b0102.mp4', NULL, 2, CAST(N'2026-07-14T15:29:53.133' AS DateTime), 28, NULL, N'22:08', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (37, N'Bài 4. Giới thiệu về carbohydrate. Glucose và fructose', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784018813/videos/4812a6f8-b613-4cf8-863b-aa45ba76d1a9.mp4', NULL, 1, CAST(N'2026-07-14T15:46:54.797' AS DateTime), 29, NULL, N'32:00', 0)
INSERT Lessons (lesson_id, lesson_title, lesson_description, video_url, subtitle_url, lesson_order, created_at, chapter_id, course_id, duration, is_preview) VALUES (38, N'Bài 5: Saccharose và maltose', N'', N'https://res.cloudinary.com/desnyjgxp/video/upload/v1784018866/videos/f50caa58-4957-4b34-8578-898055351c4a.mp4', NULL, 2, CAST(N'2026-07-14T15:47:47.680' AS DateTime), 29, NULL, N'17:45', 0)SET IDENTITY_INSERT Lessons OFF
SET IDENTITY_INSERT Lessons OFF

SET IDENTITY_INSERT LearningMaterials ON 
INSERT LearningMaterials (material_id, lesson_id, material_title, file_url, uploaded_at) VALUES (8, 14, N'Trắc nghiệm Tính đơn điệu và cực trị của hàm số', N'https://res.cloudinary.com/desnyjgxp/raw/upload/v1783859873/materials/bfea7dd9-cd88-4e37-bb8e-56364eea4d98', CAST(N'2026-07-12T19:37:54.277' AS DateTime))
SET IDENTITY_INSERT LearningMaterials OFF

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

--TỰ LUẬN
DECLARE @ValidCourseId INT = (SELECT TOP 1 course_id FROM Courses);

INSERT INTO Quizzes (course_id, quiz_title, duration_minutes, quiz_type, created_at)
VALUES (@ValidCourseId, N'Đề Test Toàn Diện Hệ Thống Chấm Điểm 2026', 90, 'MOCK_TEST', GETDATE());
----------------------------
-- ===========================================================================
-- 6. CÂU HỎI & ĐÁP ÁN (Dùng Variables tránh lỗi trùng ID)
-- ===========================================================================
GO
DECLARE @QuizId INT;
DECLARE @QId INT;

-- ---------------------------------------------------------
-- QUIZ 1: Đề thi thử Toán Đề 1
-- ---------------------------------------------------------
SET @QuizId = 1;

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId, N'Tìm đạo hàm của hàm số y = x^3 - 3x.', '3x^2 - 3',
N'y'' = (x^3)'' - (3x)'' = 3x^2 - 3');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'3x^2 - 3', 1),
(@QId, N'3x^2', 0),
(@QId, N'x^2 - 3', 0),
(@QId, N'3x^2 + 3', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId, N'Tìm đường tiệm cận đứng của đồ thị hàm số y = (2x + 1)/(x - 1).',
'x = 1',
N'Tiệm cận đứng là nghiệm của mẫu số: x - 1 = 0 => x = 1');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'x = 1', 1),
(@QId, N'y = 2', 0),
(@QId, N'x = -1', 0),
(@QId, N'y = -1', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Hàm số y = x^4 - 2x^2 đạt cực tiểu tại điểm nào?',
'x = 1 và x = -1',
N'y'' = 4x^3 - 4x = 0 => x = 0, x = ±1. Hai điểm cực tiểu là x = ±1');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'x = 1 và x = -1', 1),
(@QId, N'x = 0', 0),
(@QId, N'x = 2', 0),
(@QId, N'x = -2', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Tìm nguyên hàm của hàm số f(x) = cos(x).',
'sin(x) + C',
N'Theo bảng nguyên hàm cơ bản.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'sin(x) + C', 1),
(@QId, N'-sin(x) + C', 0),
(@QId, N'tan(x) + C', 0),
(@QId, N'-cos(x) + C', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Tính tích phân I từ 0 đến 1 của e^x dx.',
'e - 1',
N'e¹ - e⁰ = e - 1');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'e - 1', 1),
(@QId, N'e', 0),
(@QId, N'e + 1', 0),
(@QId, N'1', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Tính thể tích khối lập phương cạnh 2a.',
'8a^3',
N'V = (2a)^3');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'8a^3', 1),
(@QId, N'2a^3', 0),
(@QId, N'4a^3', 0),
(@QId, N'a^3', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Công thức tính thể tích khối chóp?',
'V = (1/3)Bh',
N'V = 1/3 diện tích đáy × chiều cao.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'V = (1/3)Bh', 1),
(@QId, N'V = Bh', 0),
(@QId, N'V = 3Bh', 0),
(@QId, N'V = (1/2)Bh', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Giải phương trình log3(x - 1) = 2.',
'x = 10',
N'3² = 9 ⇒ x = 10');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'x = 10', 1),
(@QId, N'x = 7', 0),
(@QId, N'x = 8', 0),
(@QId, N'x = 9', 0);



INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Giải phương trình 2^(x+1)=8.',
'x = 2',
N'8 = 2³');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'x = 2', 1),
(@QId, N'x = 3', 0),
(@QId, N'x = 1', 0),
(@QId, N'x = 4', 0);

INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Cho số phức z = 3 + 4i. Tính môđun của z.',
'5',
N'√(3² + 4²) = 5');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'5', 1),
(@QId, N'7', 0),
(@QId, N'25', 0),
(@QId, N'căn(7)', 0);

-- ---------------------------------------------------------
-- QUIZ 3: Đề thi thử Vật Lý
-- ---------------------------------------------------------
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

-- Câu 1
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'She _____ to the cinema with her friends last night.',
'went',
N'Dấu hiệu "last night" chia thì quá khứ đơn (V2 của go là went).');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'went', 1),
(@QId, N'goes', 0),
(@QId, N'has gone', 0),
(@QId, N'was going', 0);

-- Câu 2
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'The old house _____ by the storm yesterday.',
'was destroyed',
N'Chủ ngữ là vật, có "by" và "yesterday" nên dùng bị động quá khứ đơn.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'was destroyed', 1),
(@QId, N'destroyed', 0),
(@QId, N'is destroyed', 0),
(@QId, N'was destroying', 0);

-- Câu 3
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'If I _____ you, I would accept that job offer.',
'were',
N'Câu điều kiện loại II, động từ "to be" dùng "were" cho mọi ngôi.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'were', 1),
(@QId, N'am', 0),
(@QId, N'had been', 0),
(@QId, N'will be', 0);

-- Câu 4
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'The man _____ lives next door is a famous musician.',
'who',
N'"Who" là đại từ quan hệ thay thế cho danh từ chỉ người làm chủ ngữ.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'who', 1),
(@QId, N'whom', 0),
(@QId, N'which', 0),
(@QId, N'whose', 0);

-- Câu 5
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'This smartphone is much _____ than my old one.',
'more expensive',
N'So sánh hơn của tính từ dài dùng "more + adjective".');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'more expensive', 1),
(@QId, N'expensive', 0),
(@QId, N'most expensive', 0),
(@QId, N'as expensive', 0);

-- Câu 6
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Hardly _____ the meeting when the CEO announced the company''s new strategy.',
N'had we started',
N'Cấu trúc đảo ngữ: Hardly had + S + V3/ed + when...');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'had we started', 1),
(@QId, N'we had started', 0),
(@QId, N'did we start', 0),
(@QId, N'we started', 0);

-- Câu 7
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'The proposal was rejected, _____ surprised everyone in the committee.',
N'which',
N'"Which" thay thế cho cả mệnh đề đứng trước.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'which', 1),
(@QId, N'who', 0),
(@QId, N'where', 0),
(@QId, N'what', 0);

-- Câu 8
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'By the time we arrived at the station, the train _____.',
N'had already left',
N'Hành động xảy ra trước một hành động khác trong quá khứ → Past Perfect.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'had already left', 1),
(@QId, N'has already left', 0),
(@QId, N'already left', 0),
(@QId, N'was leaving', 0);

-- Câu 9
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'It is essential that every student _____ the assignment before Friday.',
N'submit',
N'Sau "It is essential that" dùng động từ nguyên mẫu (subjunctive mood).');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'submit', 1),
(@QId, N'submits', 0),
(@QId, N'submitted', 0),
(@QId, N'will submit', 0);

-- Câu 10
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'The more she practised speaking English, _____.',
N'the more confident she became',
N'Cấu trúc so sánh kép: The more..., the more...');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'the more confident she became', 1),
(@QId, N'she became more confident', 0),
(@QId, N'the most confident she became', 0),
(@QId, N'more confident she became', 0);
-- ---------------------------------------------------------
-- QUIZ 6: Từ vựng và cấu trúc câu
-- ---------------------------------------------------------
SET @QuizId = 6;

-- =====================================================
-- Câu 1 (Dễ)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'I am looking forward to _____ from you soon.',
'hearing',
N'look forward to + V-ing.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'hearing', 1),
(@QId, N'hear', 0),
(@QId, N'heard', 0),
(@QId, N'to hear', 0);

-- =====================================================
-- Câu 2 (Dễ)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'You need to _____ a decision right now before it''s too late.',
'make',
N'Cụm cố định: make a decision.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'make', 1),
(@QId, N'do', 0),
(@QId, N'take', 0),
(@QId, N'give', 0);

-- =====================================================
-- Câu 3 (Dễ)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Effective _____ is the key to a successful relationship.',
'communication',
N'Sau tính từ "effective" cần một danh từ.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'communication', 1),
(@QId, N'communicate', 0),
(@QId, N'communicative', 0),
(@QId, N'communicated', 0);

-- =====================================================
-- Câu 4 (Dễ)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Despite the difficulties, he remained _____ about the future.',
'optimistic',
N'Optimistic = lạc quan.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'optimistic', 1),
(@QId, N'pessimistic', 0),
(@QId, N'hopeless', 0),
(@QId, N'disappointed', 0);

-- =====================================================
-- Câu 5 (Dễ)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Neither my brother nor my parents _____ at home yesterday.',
'were',
N'Neither...nor... chia theo chủ ngữ gần nhất (parents).');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'were', 1),
(@QId, N'was', 0),
(@QId, N'are', 0),
(@QId, N'is', 0);

-- =====================================================
-- Câu 6 (Trung bình)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'The project _____ by the time the manager arrived.',
'had been completed',
N'Past Perfect Passive.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'had been completed', 1),
(@QId, N'has completed', 0),
(@QId, N'was completing', 0),
(@QId, N'completed', 0);

-- =====================================================
-- Câu 7 (Trung bình)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'If she _____ harder, she would have passed the final exam.',
'had studied',
N'Câu điều kiện loại III.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'had studied', 1),
(@QId, N'studied', 0),
(@QId, N'has studied', 0),
(@QId, N'would study', 0);

-- =====================================================
-- Câu 8 (Trung bình)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'The woman _____ purse was stolen reported the incident to the police.',
'whose',
N'Whose diễn tả sự sở hữu.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'whose', 1),
(@QId, N'who', 0),
(@QId, N'whom', 0),
(@QId, N'which', 0);

-- =====================================================
-- Câu 9 (Khó)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'Rarely _____ such an impressive performance.',
'have I seen',
N'Đảo ngữ với Rarely.');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'have I seen', 1),
(@QId, N'I have seen', 0),
(@QId, N'did I see', 0),
(@QId, N'I saw', 0);

-- =====================================================
-- Câu 10 (Khó)
-- =====================================================
INSERT INTO Questions (quiz_id, question_content, correct_answer, explanation)
VALUES (@QuizId,
N'No sooner _____ home than it started to rain heavily.',
'had we arrived',
N'Cấu trúc đảo ngữ: No sooner had + S + V3 than...');
SET @QId = SCOPE_IDENTITY();

INSERT INTO QuestionOptions (question_id, option_content, is_correct)
VALUES
(@QId, N'had we arrived', 1),
(@QId, N'we had arrived', 0),
(@QId, N'did we arrive', 0),
(@QId, N'we arrived', 0);
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

--StudySchedule
INSERT INTO StudySchedules (user_id, title, schedule_date, schedule_time, schedule_type)
VALUES 
(5, N'Luyện đề Toán số 1', '2026-07-15', '19:00', 'math'),
(5, N'Kiểm tra Lý 45p', '2026-07-20', '14:00', 'physics'),
(5, N'Livestream Tiếng Anh', '2026-07-25', '20:00', 'english');

-- Kiểm tra lại schema
PRINT '=== Hoàn tất quá trình tạo Data (Merged SQL) ===';
GO

SELECT * FROM Users
-- ===========================================================================
-- 9. BỔ SUNG CẤU TRÚC VÀ DỮ LIỆU ĐỀ THI THỬ TỰ LUẬN HỖN HỢP (NÂNG CẤP 2026)
-- ===========================================================================
-- 1. Bổ sung các cột lưu điểm và lời phê tự luận cho bảng Luyện đề mới (PracticeAnswers)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('PracticeAnswers') AND name = 'essay_answer')
    ALTER TABLE PracticeAnswers ADD essay_answer NVARCHAR(MAX) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('PracticeAnswers') AND name = 'score')
    ALTER TABLE PracticeAnswers ADD score FLOAT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('PracticeAnswers') AND name = 'teacher_comment')
    ALTER TABLE PracticeAnswers ADD teacher_comment NVARCHAR(MAX) NULL;
GO

-- 2. Quét và xoá sạch các Check Constraint cũ chặn trạng thái 'PENDING_GRADING' của QuizAttempts
DECLARE @sql NVARCHAR(MAX) = '';
SELECT @sql += 'ALTER TABLE QuizAttempts DROP CONSTRAINT ' + name + ';' + CHAR(13)
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('QuizAttempts');

EXEC sp_executesql @sql;
GO

-- 3. Tạo lại ràng buộc mới chuẩn đét cho phép lưu trạng thái Chờ chấm điểm
ALTER TABLE QuizAttempts ADD CONSTRAINT CK_QuizAttempts_status_new CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED', 'PENDING_GRADING'));
GO

-- 4. TẠO MỘT ĐỀ THI MẪU PHỐI HỢP CẢ 3 THỂ LOẠI ĐỂ TEST ENGINE CHẤM ĐIỂM
DECLARE @ValidCourseId INT = (SELECT TOP 1 course_id FROM Courses);

INSERT INTO Quizzes (course_id, quiz_title, duration_minutes, quiz_type, created_at)
VALUES (@ValidCourseId, N'Đề Test Toàn Diện Hệ Thống Chấm Điểm 2026', 90, 'PRACTICE', GETDATE());

DECLARE @NewQuizId INT = SCOPE_IDENTITY();

-- Thể loại 1: Trắc nghiệm truyền thống (CHOICE)
INSERT INTO Questions (quiz_id, question_content, question_type, correct_answer, explanation)
VALUES (@NewQuizId, N'Tính đạo hàm của hàm số y = x³ - 3x.', 'CHOICE', N'3x² - 3', N'Áp dụng công thức tính đạo hàm cơ bản (x^n)'' = n*x^(n-1).');

DECLARE @QuestionChoiceId INT = SCOPE_IDENTITY();
INSERT INTO QuestionOptions (question_id, option_content, is_correct) VALUES 
(@QuestionChoiceId, N'3x² - 3', 1), -- Đáp án đúng
(@QuestionChoiceId, N'3x² + 3', 0), 
(@QuestionChoiceId, N'x² - 3', 0),
(@QuestionChoiceId, N'3x³ - 3', 0);

-- Thể loại 2: Đáp án ngắn máy tự chấm (SHORT_ANSWER)
INSERT INTO Questions (quiz_id, question_content, question_type, correct_answer, explanation)
VALUES (@NewQuizId, N'Tìm số giao điểm của đồ thị hàm số y = x³ - 3x và trục hoành.', 'SHORT_ANSWER', N'3', N'Phương trình hoành độ giao điểm x³ - 3x = 0 <=> x(x² - 3) = 0 => x = 0 hoặc x = ±√3. Vậy có 3 nghiệm (3 giao điểm).');

-- Thể loại 3: Tự luận giáo viên chấm (ESSAY)
INSERT INTO Questions (quiz_id, question_content, question_type, correct_answer, explanation)
VALUES (@NewQuizId, N'Viết phương trình tiếp tuyến của đồ thị (C): y = x³ - 3x tại điểm có hoành độ x₀ = 2.', 'ESSAY', N'y = 9x - 10', N'Ta có đạo hàm y phẩy tại 2 bằng 9, y(2) = 2. Phương trình tiếp tuyến là: y = 9(x - 2) + 2 <=> y = 9x - 10.');
GO

-- Kiểm tra lại sản phẩm sau khi nâng cấp
SELECT * FROM Quizzes WHERE quiz_title LIKE N'%Hệ Thống Chấm Điểm%';
SELECT * FROM Questions WHERE quiz_id = (SELECT MAX(quiz_id) FROM Quizzes);

SELECT * FROM Courses
SELECT *
FROM StudentAnswers

ALTER TABLE Questions ALTER COLUMN question_content NVARCHAR(MAX);
ALTER TABLE QuestionOptions ALTER COLUMN option_content NVARCHAR(MAX);
