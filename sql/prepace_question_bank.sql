/* Tự động sinh từ gen_question_bank.py — Mở rộng kho đề Kiểm tra đầu vào */
USE [PrepAce];
GO
SET NOCOUNT ON;
GO

IF COL_LENGTH('dbo.Quizzes','quiz_type') IS NULL ALTER TABLE dbo.Quizzes ADD quiz_type NVARCHAR(50) NULL;
IF COL_LENGTH('dbo.Questions','cognitive_level') IS NULL ALTER TABLE dbo.Questions ADD cognitive_level INT NULL;
GO

DECLARE @chem INT, @lit INT;
SELECT @chem = course_id FROM dbo.Courses WHERE course_title = N'Hóa học 12 - Luyện thi THPT QG';
IF @chem IS NULL BEGIN
  INSERT INTO dbo.Courses (teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at)
  VALUES (3, 3, N'Hóa học 12 - Luyện thi THPT QG', N'Khóa học Hóa học toàn diện ôn thi THPT Quốc gia.', N'/uploads/thumbnails/chem-course.jpg', 499000, 1, GETDATE());
  SET @chem = SCOPE_IDENTITY();
END
SELECT @lit = course_id FROM dbo.Courses WHERE course_title = N'Ngữ văn 12 - Ôn luyện THPT QG';
IF @lit IS NULL BEGIN
  INSERT INTO dbo.Courses (teacher_id, subject_id, course_title, course_description, thumbnail_url, price, is_published, created_at)
  VALUES (4, 4, N'Ngữ văn 12 - Ôn luyện THPT QG', N'Khóa học Ngữ văn bám sát cấu trúc đề thi THPT Quốc gia.', N'/uploads/thumbnails/lit-course.jpg', 399000, 1, GETDATE());
  SET @lit = SCOPE_IDENTITY();
END
GO

DELETE o FROM dbo.QuestionOptions o JOIN dbo.Questions q ON o.question_id=q.question_id JOIN dbo.Quizzes z ON q.quiz_id=z.quiz_id WHERE z.quiz_type='ENTRY_TEST';
DELETE qa FROM dbo.QuizAttempts qa JOIN dbo.Quizzes z ON qa.quiz_id=z.quiz_id WHERE z.quiz_type='ENTRY_TEST';
DELETE qq FROM dbo.Questions qq JOIN dbo.Quizzes z ON qq.quiz_id=z.quiz_id WHERE z.quiz_type='ENTRY_TEST';
DELETE FROM dbo.Quizzes WHERE quiz_type='ENTRY_TEST';
GO

DECLARE @chem INT, @lit INT, @qz INT, @qid INT;
SELECT @chem = course_id FROM dbo.Courses WHERE course_title = N'Hóa học 12 - Luyện thi THPT QG';
SELECT @lit  = course_id FROM dbo.Courses WHERE course_title = N'Ngữ văn 12 - Ôn luyện THPT QG';

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES (1, N'Kiểm tra đầu vào — Toán (Cơ bản)', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Đạo hàm của hàm số y = x³ là?', N'3x²', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3x²'),(@qid,N'x²'),(@qid,N'3x'),(@qid,N'x⁴');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'log₂(16) bằng bao nhiêu?', N'4', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'4'),(@qid,N'2'),(@qid,N'8'),(@qid,N'3');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Nghiệm của phương trình x² - 9 = 0 là?', N'x = ±3', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x = ±3'),(@qid,N'x = 3'),(@qid,N'x = 9'),(@qid,N'x = ±9');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'2⁵ bằng bao nhiêu?', N'32', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'32'),(@qid,N'16'),(@qid,N'25'),(@qid,N'10');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Đạo hàm của y = sin x là?', N'cos x', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'cos x'),(@qid,N'-cos x'),(@qid,N'sin x'),(@qid,N'-sin x');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tập xác định của hàm số y = 1/x là?', N'x ≠ 0', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x ≠ 0'),(@qid,N'x > 0'),(@qid,N'mọi x'),(@qid,N'x < 0');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Giá trị của 0! (giai thừa) là?', N'1', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'Không xác định'),(@qid,N'Vô cực');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Nghiệm của phương trình 2x + 6 = 0 là?', N'x = -3', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x = -3'),(@qid,N'x = 3'),(@qid,N'x = -6'),(@qid,N'x = 6');

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES (1, N'Kiểm tra đầu vào — Toán (Nâng cao)', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tích phân ∫2x dx bằng?', N'x² + C', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x² + C'),(@qid,N'2 + C'),(@qid,N'x²'),(@qid,N'2x² + C');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Giá trị lim(x→0) sin(x)/x là?', N'1', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'Vô cực'),(@qid,N'Không tồn tại');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Số nghiệm phân biệt của x³ - 3x + 2 = 0 là?', N'2', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'2'),(@qid,N'1'),(@qid,N'3'),(@qid,N'0');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Phương trình tiếp tuyến của y = x² tại x = 1 là?', N'y = 2x - 1', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'y = 2x - 1'),(@qid,N'y = 2x + 1'),(@qid,N'y = x - 1'),(@qid,N'y = 2x');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Giá trị lớn nhất của y = -x² + 4x - 1 là?', N'3', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3'),(@qid,N'4'),(@qid,N'1'),(@qid,N'-1');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tổ hợp C(6,2) bằng?', N'15', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'15'),(@qid,N'12'),(@qid,N'30'),(@qid,N'20');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Đạo hàm của y = ln x là?', N'1/x', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1/x'),(@qid,N'ln x'),(@qid,N'x'),(@qid,N'eˣ');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tổng cấp số nhân lùi vô hạn 1 + 1/2 + 1/4 + ... bằng?', N'2', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'2'),(@qid,N'1'),(@qid,N'Vô cực'),(@qid,N'1.5');

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES (2, N'Kiểm tra đầu vào — Vật lý', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Đơn vị của công suất là?', N'Watt', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Watt'),(@qid,N'Joule'),(@qid,N'Newton'),(@qid,N'Pascal');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Theo định luật II Newton, gia tốc a bằng?', N'F/m', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'F/m'),(@qid,N'm/F'),(@qid,N'F·m'),(@qid,N'F + m');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tốc độ ánh sáng trong chân không xấp xỉ?', N'3×10⁸ m/s', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3×10⁸ m/s'),(@qid,N'3×10⁶ m/s'),(@qid,N'340 m/s'),(@qid,N'9,8 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Chu kì con lắc đơn phụ thuộc vào?', N'Chiều dài và gia tốc trọng trường', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Chiều dài và gia tốc trọng trường'),(@qid,N'Khối lượng vật'),(@qid,N'Biên độ dao động'),(@qid,N'Vận tốc ban đầu');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Công thức tính động năng là?', N'½mv²', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'½mv²'),(@qid,N'mgh'),(@qid,N'mv'),(@qid,N'½kx²');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Đơn vị của điện trở là?', N'Ôm (Ω)', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Ôm (Ω)'),(@qid,N'Vôn'),(@qid,N'Ampe'),(@qid,N'Fara');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Vật rơi tự do (g = 10 m/s²), sau 3s vận tốc là?', N'30 m/s', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'30 m/s'),(@qid,N'10 m/s'),(@qid,N'90 m/s'),(@qid,N'3 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Hiện tượng giao thoa chứng tỏ ánh sáng có tính chất?', N'Sóng', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Sóng'),(@qid,N'Hạt'),(@qid,N'Điện'),(@qid,N'Từ');

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES (3, N'Kiểm tra đầu vào — Tiếng Anh', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'She _____ to school every day.', N'goes', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'goes'),(@qid,N'go'),(@qid,N'going'),(@qid,N'gone');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'I have lived here _____ 2010.', N'since', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'since'),(@qid,N'for'),(@qid,N'from'),(@qid,N'at');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Choose the synonym of ''big''.', N'large', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'large'),(@qid,N'small'),(@qid,N'tiny'),(@qid,N'short');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'If I _____ you, I would study harder.', N'were', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'were'),(@qid,N'am'),(@qid,N'was'),(@qid,N'will be');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'The book _____ I read was interesting.', N'that', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'that'),(@qid,N'who'),(@qid,N'where'),(@qid,N'when');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Choose the past tense of ''go''.', N'went', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'went'),(@qid,N'gone'),(@qid,N'goed'),(@qid,N'going');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'''Make a decision'' means to _____.', N'decide', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'decide'),(@qid,N'delay'),(@qid,N'refuse'),(@qid,N'forget');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'He is interested _____ music.', N'in', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'in'),(@qid,N'on'),(@qid,N'at'),(@qid,N'of');

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES (@chem, N'Kiểm tra đầu vào — Hóa học', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Kí hiệu hóa học của nguyên tố Natri là?', N'Na', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Na'),(@qid,N'N'),(@qid,N'So'),(@qid,N'Nu');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Công thức hóa học của nước là?', N'H₂O', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'H₂O'),(@qid,N'CO₂'),(@qid,N'O₂'),(@qid,N'H₂');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Nguyên tử Cacbon (Z=6) có bao nhiêu proton?', N'6', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'6'),(@qid,N'12'),(@qid,N'8'),(@qid,N'4');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Dung dịch trung tính có pH bằng?', N'7', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'7'),(@qid,N'0'),(@qid,N'14'),(@qid,N'1');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Khí nào là tác nhân chính gây hiệu ứng nhà kính?', N'CO₂', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'CO₂'),(@qid,N'O₂'),(@qid,N'N₂'),(@qid,N'H₂');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Kim loại nào nhẹ nhất trong các kim loại sau?', N'Liti', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Liti'),(@qid,N'Sắt'),(@qid,N'Vàng'),(@qid,N'Đồng');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Phản ứng tỏa nhiệt là phản ứng?', N'Giải phóng nhiệt ra môi trường', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Giải phóng nhiệt ra môi trường'),(@qid,N'Thu nhiệt từ môi trường'),(@qid,N'Không thay đổi nhiệt'),(@qid,N'Hấp thụ ánh sáng');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Hóa trị của Oxi trong hợp chất H₂O là?', N'II', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'II'),(@qid,N'I'),(@qid,N'III'),(@qid,N'IV');

INSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES (@lit, N'Kiểm tra đầu vào — Ngữ văn', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tác phẩm ''Truyện Kiều'' là của tác giả nào?', N'Nguyễn Du', NULL, 1); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Nguyễn Du'),(@qid,N'Hồ Xuân Hương'),(@qid,N'Nguyễn Trãi'),(@qid,N'Tố Hữu');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'''Truyện Kiều'' được viết theo thể thơ nào?', N'Lục bát', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Lục bát'),(@qid,N'Song thất lục bát'),(@qid,N'Thất ngôn bát cú'),(@qid,N'Thơ tự do');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'''Mặt trời xuống biển như hòn lửa'' sử dụng biện pháp tu từ nào?', N'So sánh', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'So sánh'),(@qid,N'Ẩn dụ'),(@qid,N'Nhân hóa'),(@qid,N'Hoán dụ');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Tác phẩm ''Vợ nhặt'' là của nhà văn nào?', N'Kim Lân', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Kim Lân'),(@qid,N'Nam Cao'),(@qid,N'Ngô Tất Tố'),(@qid,N'Vũ Trọng Phụng');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Bài thơ ''Tây Tiến'' là của nhà thơ nào?', N'Quang Dũng', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Quang Dũng'),(@qid,N'Tố Hữu'),(@qid,N'Xuân Diệu'),(@qid,N'Chế Lan Viên');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Phương thức biểu đạt chính của văn nghị luận là?', N'Nghị luận', NULL, 3); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Nghị luận'),(@qid,N'Tự sự'),(@qid,N'Miêu tả'),(@qid,N'Biểu cảm');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Bài thơ ''Sóng'' là của nữ sĩ nào?', N'Xuân Quỳnh', NULL, 2); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Xuân Quỳnh'),(@qid,N'Hồ Xuân Hương'),(@qid,N'Anh Thơ'),(@qid,N'Bà Huyện Thanh Quan');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'Nhân vật Chí Phèo xuất hiện trong tác phẩm của?', N'Nam Cao', NULL, 4); SET @qid = SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Nam Cao'),(@qid,N'Kim Lân'),(@qid,N'Ngô Tất Tố'),(@qid,N'Vũ Trọng Phụng');

GO
PRINT '✅ Mở rộng kho đề Kiểm tra đầu vào hoàn tất.';
GO