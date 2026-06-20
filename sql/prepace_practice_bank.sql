/* Tự sinh từ gen_practice_bank.py — nạp lại câu hỏi+đáp án cho đề Luyện đề/Thi thử */
USE [PrepAce];
GO
SET NOCOUNT ON;
GO

/* Xóa an toàn theo thứ tự khóa ngoại (chỉ các quiz luyện đề/thi thử) */
DELETE sa FROM dbo.StudentAnswers sa JOIN dbo.Questions q ON sa.question_id=q.question_id WHERE q.quiz_id IN (1,2,3,4,5,6,7,8,9);
DELETE o  FROM dbo.QuestionOptions o JOIN dbo.Questions q ON o.question_id=q.question_id WHERE q.quiz_id IN (1,2,3,4,5,6,7,8,9);
DELETE FROM dbo.Questions WHERE quiz_id IN (1,2,3,4,5,6,7,8,9);
GO

DECLARE @qid INT;

/* ===== Quiz 1 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (1, N'Đạo hàm của hàm số y = x² là?', N'2x', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'2x'),(@qid,N'x²'),(@qid,N'2'),(@qid,N'x');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (1, N'Đạo hàm của hàm số y = x³ là?', N'3x²', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3x²'),(@qid,N'x²'),(@qid,N'3x'),(@qid,N'2x');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (1, N'Nguyên hàm của f(x) = 2x là?', N'x² + C', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x² + C'),(@qid,N'2 + C'),(@qid,N'x³ + C'),(@qid,N'2x² + C');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (1, N'log₂(8) bằng bao nhiêu?', N'3', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3'),(@qid,N'2'),(@qid,N'4'),(@qid,N'8');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (1, N'Nghiệm của phương trình x² - 5x + 6 = 0 là?', N'x = 2 hoặc x = 3', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x = 2 hoặc x = 3'),(@qid,N'x = 1 hoặc x = 6'),(@qid,N'x = -2 hoặc x = -3'),(@qid,N'Vô nghiệm');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (1, N'Giá trị lim(x→0) sin(x)/x là?', N'1', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'Vô cực'),(@qid,N'Không tồn tại');

/* ===== Quiz 2 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (2, N'Đơn vị của lực trong hệ SI là?', N'Newton', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Newton'),(@qid,N'Joule'),(@qid,N'Watt'),(@qid,N'Pascal');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (2, N'Theo định luật II Newton, gia tốc a bằng?', N'F/m', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'F/m'),(@qid,N'm/F'),(@qid,N'F·m'),(@qid,N'F + m');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (2, N'Đơn vị của công suất là?', N'Watt', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Watt'),(@qid,N'Joule'),(@qid,N'Newton'),(@qid,N'Ampe');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (2, N'Công thức tính động năng là?', N'½mv²', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'½mv²'),(@qid,N'mgh'),(@qid,N'mv'),(@qid,N'½kx²');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (2, N'Tốc độ ánh sáng trong chân không xấp xỉ?', N'3×10⁸ m/s', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3×10⁸ m/s'),(@qid,N'3×10⁶ m/s'),(@qid,N'340 m/s'),(@qid,N'9,8 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (2, N'Vật rơi tự do (g = 10 m/s²), sau 2s vận tốc là?', N'20 m/s', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'20 m/s'),(@qid,N'10 m/s'),(@qid,N'40 m/s'),(@qid,N'5 m/s');

/* ===== Quiz 3 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (3, N'She _____ to school every day.', N'goes', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'goes'),(@qid,N'go'),(@qid,N'going'),(@qid,N'gone');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (3, N'I have lived here _____ 2015.', N'since', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'since'),(@qid,N'for'),(@qid,N'from'),(@qid,N'at');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (3, N'Choose the synonym of ''happy''.', N'joyful', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'joyful'),(@qid,N'sad'),(@qid,N'angry'),(@qid,N'tired');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (3, N'If I _____ rich, I would travel the world.', N'were', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'were'),(@qid,N'am'),(@qid,N'was'),(@qid,N'will be');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (3, N'The book _____ I read was interesting.', N'that', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'that'),(@qid,N'who'),(@qid,N'where'),(@qid,N'when');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (3, N'Choose the past tense of ''go''.', N'went', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'went'),(@qid,N'gone'),(@qid,N'goed'),(@qid,N'going');

/* ===== Quiz 4 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'Nguyên hàm của f(x) = 2x là?', N'x² + C', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x² + C'),(@qid,N'2 + C'),(@qid,N'x³ + C'),(@qid,N'2x² + C');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'log₂(8) bằng bao nhiêu?', N'3', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3'),(@qid,N'2'),(@qid,N'4'),(@qid,N'8');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'Nghiệm của phương trình x² - 5x + 6 = 0 là?', N'x = 2 hoặc x = 3', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x = 2 hoặc x = 3'),(@qid,N'x = 1 hoặc x = 6'),(@qid,N'x = -2 hoặc x = -3'),(@qid,N'Vô nghiệm');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'Giá trị lim(x→0) sin(x)/x là?', N'1', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'Vô cực'),(@qid,N'Không tồn tại');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'Tích phân ∫x² dx bằng?', N'x³/3 + C', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x³/3 + C'),(@qid,N'2x + C'),(@qid,N'x³ + C'),(@qid,N'3x² + C');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'Giá trị nhỏ nhất của y = x² - 4x + 5 là?', N'1', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'5'),(@qid,N'-1');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'Tổ hợp C(5,2) bằng?', N'10', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'10'),(@qid,N'20'),(@qid,N'5'),(@qid,N'25');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (4, N'2¹⁰ bằng bao nhiêu?', N'1024', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1024'),(@qid,N'512'),(@qid,N'256'),(@qid,N'2048');

/* ===== Quiz 5 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Đạo hàm của hàm số y = x² là?', N'2x', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'2x'),(@qid,N'x²'),(@qid,N'2'),(@qid,N'x');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Đạo hàm của hàm số y = x³ là?', N'3x²', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3x²'),(@qid,N'x²'),(@qid,N'3x'),(@qid,N'2x');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Nguyên hàm của f(x) = 2x là?', N'x² + C', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x² + C'),(@qid,N'2 + C'),(@qid,N'x³ + C'),(@qid,N'2x² + C');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'log₂(8) bằng bao nhiêu?', N'3', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3'),(@qid,N'2'),(@qid,N'4'),(@qid,N'8');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Nghiệm của phương trình x² - 5x + 6 = 0 là?', N'x = 2 hoặc x = 3', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x = 2 hoặc x = 3'),(@qid,N'x = 1 hoặc x = 6'),(@qid,N'x = -2 hoặc x = -3'),(@qid,N'Vô nghiệm');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Giá trị lim(x→0) sin(x)/x là?', N'1', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'Vô cực'),(@qid,N'Không tồn tại');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Tích phân ∫x² dx bằng?', N'x³/3 + C', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'x³/3 + C'),(@qid,N'2x + C'),(@qid,N'x³ + C'),(@qid,N'3x² + C');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Giá trị nhỏ nhất của y = x² - 4x + 5 là?', N'1', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1'),(@qid,N'0'),(@qid,N'5'),(@qid,N'-1');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'Tổ hợp C(5,2) bằng?', N'10', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'10'),(@qid,N'20'),(@qid,N'5'),(@qid,N'25');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (5, N'2¹⁰ bằng bao nhiêu?', N'1024', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'1024'),(@qid,N'512'),(@qid,N'256'),(@qid,N'2048');

/* ===== Quiz 6 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Đơn vị của lực trong hệ SI là?', N'Newton', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Newton'),(@qid,N'Joule'),(@qid,N'Watt'),(@qid,N'Pascal');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Theo định luật II Newton, gia tốc a bằng?', N'F/m', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'F/m'),(@qid,N'm/F'),(@qid,N'F·m'),(@qid,N'F + m');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Đơn vị của công suất là?', N'Watt', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Watt'),(@qid,N'Joule'),(@qid,N'Newton'),(@qid,N'Ampe');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Công thức tính động năng là?', N'½mv²', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'½mv²'),(@qid,N'mgh'),(@qid,N'mv'),(@qid,N'½kx²');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Tốc độ ánh sáng trong chân không xấp xỉ?', N'3×10⁸ m/s', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3×10⁸ m/s'),(@qid,N'3×10⁶ m/s'),(@qid,N'340 m/s'),(@qid,N'9,8 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Vật rơi tự do (g = 10 m/s²), sau 2s vận tốc là?', N'20 m/s', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'20 m/s'),(@qid,N'10 m/s'),(@qid,N'40 m/s'),(@qid,N'5 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Đơn vị của điện trở là?', N'Ôm (Ω)', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Ôm (Ω)'),(@qid,N'Vôn'),(@qid,N'Ampe'),(@qid,N'Fara');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (6, N'Chu kì con lắc đơn phụ thuộc vào?', N'Chiều dài và gia tốc trọng trường', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Chiều dài và gia tốc trọng trường'),(@qid,N'Khối lượng vật'),(@qid,N'Biên độ dao động'),(@qid,N'Vận tốc ban đầu');

/* ===== Quiz 7 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Đơn vị của công suất là?', N'Watt', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Watt'),(@qid,N'Joule'),(@qid,N'Newton'),(@qid,N'Ampe');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Công thức tính động năng là?', N'½mv²', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'½mv²'),(@qid,N'mgh'),(@qid,N'mv'),(@qid,N'½kx²');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Tốc độ ánh sáng trong chân không xấp xỉ?', N'3×10⁸ m/s', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'3×10⁸ m/s'),(@qid,N'3×10⁶ m/s'),(@qid,N'340 m/s'),(@qid,N'9,8 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Vật rơi tự do (g = 10 m/s²), sau 2s vận tốc là?', N'20 m/s', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'20 m/s'),(@qid,N'10 m/s'),(@qid,N'40 m/s'),(@qid,N'5 m/s');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Đơn vị của điện trở là?', N'Ôm (Ω)', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Ôm (Ω)'),(@qid,N'Vôn'),(@qid,N'Ampe'),(@qid,N'Fara');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Chu kì con lắc đơn phụ thuộc vào?', N'Chiều dài và gia tốc trọng trường', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Chiều dài và gia tốc trọng trường'),(@qid,N'Khối lượng vật'),(@qid,N'Biên độ dao động'),(@qid,N'Vận tốc ban đầu');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Đại lượng nào sau đây là vô hướng?', N'Khối lượng', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Khối lượng'),(@qid,N'Lực'),(@qid,N'Vận tốc'),(@qid,N'Gia tốc');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (7, N'Công thức thế năng trọng trường là?', N'mgh', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'mgh'),(@qid,N'½mv²'),(@qid,N'mv'),(@qid,N'F·s');

/* ===== Quiz 8 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'She _____ to school every day.', N'goes', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'goes'),(@qid,N'go'),(@qid,N'going'),(@qid,N'gone');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'I have lived here _____ 2015.', N'since', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'since'),(@qid,N'for'),(@qid,N'from'),(@qid,N'at');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'Choose the synonym of ''happy''.', N'joyful', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'joyful'),(@qid,N'sad'),(@qid,N'angry'),(@qid,N'tired');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'If I _____ rich, I would travel the world.', N'were', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'were'),(@qid,N'am'),(@qid,N'was'),(@qid,N'will be');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'The book _____ I read was interesting.', N'that', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'that'),(@qid,N'who'),(@qid,N'where'),(@qid,N'when');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'Choose the past tense of ''go''.', N'went', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'went'),(@qid,N'gone'),(@qid,N'goed'),(@qid,N'going');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'He is interested _____ music.', N'in', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'in'),(@qid,N'on'),(@qid,N'at'),(@qid,N'of');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (8, N'Choose the correct sentence: ''She _____ TV now.''', N'is watching', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'is watching'),(@qid,N'watch'),(@qid,N'watches'),(@qid,N'watched');

/* ===== Quiz 9 ===== */
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'Choose the synonym of ''happy''.', N'joyful', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'joyful'),(@qid,N'sad'),(@qid,N'angry'),(@qid,N'tired');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'If I _____ rich, I would travel the world.', N'were', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'were'),(@qid,N'am'),(@qid,N'was'),(@qid,N'will be');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'The book _____ I read was interesting.', N'that', NULL, 1); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'that'),(@qid,N'who'),(@qid,N'where'),(@qid,N'when');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'Choose the past tense of ''go''.', N'went', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'went'),(@qid,N'gone'),(@qid,N'goed'),(@qid,N'going');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'He is interested _____ music.', N'in', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'in'),(@qid,N'on'),(@qid,N'at'),(@qid,N'of');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'Choose the correct sentence: ''She _____ TV now.''', N'is watching', NULL, 3); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'is watching'),(@qid,N'watch'),(@qid,N'watches'),(@qid,N'watched');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'Choose the antonym of ''difficult''.', N'easy', NULL, 2); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'easy'),(@qid,N'hard'),(@qid,N'complex'),(@qid,N'tough');
INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (9, N'''A lot of'' can be used with?', N'Cả danh từ đếm được và không đếm được', NULL, 4); SET @qid=SCOPE_IDENTITY();
INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES (@qid,N'Cả danh từ đếm được và không đếm được'),(@qid,N'Chỉ danh từ đếm được'),(@qid,N'Chỉ danh từ không đếm được'),(@qid,N'Không từ nào');

GO
PRINT '✅ Nạp lại đề Luyện đề/Thi thử hoàn tất.';
GO