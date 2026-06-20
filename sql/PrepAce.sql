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

    FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

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

    FOREIGN KEY (teacher_id) REFERENCES Users(user_id),
    FOREIGN KEY (subject_id) REFERENCES Subjects(subject_id)
);

-- =========================
-- LESSONS
-- =========================

CREATE TABLE Lessons (
    lesson_id INT PRIMARY KEY IDENTITY(1,1),
    course_id INT NOT NULL,

    lesson_title NVARCHAR(255) NOT NULL,
    lesson_description NVARCHAR(MAX),

    video_url NVARCHAR(255),
    subtitle_url NVARCHAR(255),

    lesson_order INT,
    created_at DATETIME DEFAULT GETDATE(),

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


INSERT INTO Categories(category_name)
VALUES
('Natural Sciences'),
('Social Sciences'),
('Languages'),
('University Preparation');


INSERT INTO Subjects(subject_name, category_id)
VALUES
('Mathematics', 1),
('Physics', 1),
('Chemistry', 1),
('Literature', 2),
('English', 3),
('History', 2),
('Geography', 2)

INSERT INTO Courses
(
    teacher_id,
    subject_id,
    course_title,
    course_description,
    thumbnail_url,
    price,
    is_published
)
VALUES
(
    2,
    1,
    'Mastering Mathematics 12',
    'Complete mathematics course for National High School Exam preparation.',
    '/uploads/thumbnails/math-course.jpg',
    599000,
    1
),
(
    3,
    2,
    'Physics Problem Solving Techniques',
    'Advanced physics lessons and mock exam strategies.',
    '/uploads/thumbnails/physics-course.jpg',
    499000,
    1
),
(
    4,
    5,
    'English Vocabulary & Grammar',
    'Comprehensive English preparation for university entrance exam.',
    '/uploads/thumbnails/english-course.jpg',
    399000,
    1
);




INSERT INTO Lessons
(
    course_id,
    lesson_title,
    lesson_description,
    video_url,
    subtitle_url,
    lesson_order
)
VALUES

-- Mathematics
(
    1,
    'Derivative Basics',
    'Introduction to derivatives and formulas.',
    '/uploads/videos/math/derivative-basics.mp4',
    '/uploads/subtitles/math/derivative-basics.vtt',
    1
),
(
    1,
    'Applications of Derivatives',
    'Optimization and graph analysis.',
    '/uploads/videos/math/applications-derivatives.mp4',
    '/uploads/subtitles/math/applications-derivatives.vtt',
    2
),
(
    1,
    'Integral Fundamentals',
    'Basic integration techniques.',
    '/uploads/videos/math/integral-fundamentals.mp4',
    '/uploads/subtitles/math/integral-fundamentals.vtt',
    3
),

-- Physics
(
    2,
    'Newton Laws of Motion',
    'Force and motion concepts.',
    '/uploads/videos/physics/newton-laws.mp4',
    '/uploads/subtitles/physics/newton-laws.vtt',
    1
),
(
    2,
    'Circular Motion',
    'Uniform circular motion formulas.',
    '/uploads/videos/physics/circular-motion.mp4',
    '/uploads/subtitles/physics/circular-motion.vtt',
    2
),

-- English
(
    3,
    'Grammar Basics',
    'English grammar foundation.',
    '/uploads/videos/english/grammar-basics.mp4',
    '/uploads/subtitles/english/grammar-basics.vtt',
    1
),
(
    3,
    'Vocabulary Building',
    'Vocabulary improvement methods.',
    '/uploads/videos/english/vocabulary-building.mp4',
    '/uploads/subtitles/english/vocabulary-building.vtt',
    2
);


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



INSERT INTO Quizzes
(
    course_id,
    quiz_title,
    duration_minutes
)
VALUES
(
    1,
    'Derivative Quiz',
    30
),
(
    2,
    'Physics Mock Exam',
    60
),
(
    3,
    'English Grammar Quiz',
    25
);



INSERT INTO Questions
(
    quiz_id,
    question_content,
    correct_answer
)
VALUES
(1, 'Derivative of x^2?', '2x'),
(1, 'Derivative of sin(x)?', 'cos(x)'),

(2, 'Unit of force?', 'Newton'),
(2, 'Formula of acceleration?', 'F/m'),

(3, 'Choose correct grammar sentence.', 'A');



INSERT INTO QuestionOptions
(
    question_id,
    option_content
)
VALUES

-- Question 1
(1, '2x'),
(1, 'x'),
(1, 'x^2'),

-- Question 2
(2, 'sin(x)'),
(2, 'cos(x)'),
(2, '-cos(x)'),

-- Question 3
(3, 'Newton'),
(3, 'Pascal'),
(3, 'Joule'),

-- Question 4
(4, 'F/m'),
(4, 'm/F'),
(4, 'F+m'),

-- Question 5
(5, 'She go to school every day.'),
(5, 'She goes to school every day.'),
(5, 'She going to school every day.');



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

--ALTER TABLE
ALTER TABLE Users
ADD verification_code VARCHAR(10);

ALTER TABLE Users
ADD verification_expiry DATETIME;

ALTER TABLE Users
ADD school NVARCHAR(255),
    bio NVARCHAR(MAX);

ALTER TABLE Users ADD CONSTRAINT DF_Users_school DEFAULT NULL FOR school;
ALTER TABLE Users ADD CONSTRAINT DF_Users_bio DEFAULT NULL FOR bio;

--------------------------------------------------------------
SELECT * FROM AIChatHistory
SELECT * FROM AuditLogs
SELECT * FROM Courses
DELETE FROM Users
where user_id = 10

DELETE FROM QuizAttempts;
DBCC CHECKIDENT ('QuizAttempts', RESEED, 0);