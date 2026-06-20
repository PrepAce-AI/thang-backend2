# -*- coding: utf-8 -*-
"""Sinh file SQL mở rộng kho đề Kiểm tra đầu vào (ENTRY_TEST) cho PrepAce."""

# (course_ref, quiz_title, [ (content, [opts4], correct_index, cognitive_level), ... ])
# course_ref: 1=Toán, 2=Lý, 3=Anh, 'CHEM'=Hóa, 'LIT'=Văn
TESTS = [
 (1, "Kiểm tra đầu vào — Toán (Cơ bản)", [
   ("Đạo hàm của hàm số y = x³ là?", ["3x²","x²","3x","x⁴"], 0, 1),
   ("log₂(16) bằng bao nhiêu?", ["4","2","8","3"], 0, 1),
   ("Nghiệm của phương trình x² - 9 = 0 là?", ["x = ±3","x = 3","x = 9","x = ±9"], 0, 2),
   ("2⁵ bằng bao nhiêu?", ["32","16","25","10"], 0, 1),
   ("Đạo hàm của y = sin x là?", ["cos x","-cos x","sin x","-sin x"], 0, 1),
   ("Tập xác định của hàm số y = 1/x là?", ["x ≠ 0","x > 0","mọi x","x < 0"], 0, 2),
   ("Giá trị của 0! (giai thừa) là?", ["1","0","Không xác định","Vô cực"], 0, 1),
   ("Nghiệm của phương trình 2x + 6 = 0 là?", ["x = -3","x = 3","x = -6","x = 6"], 0, 1),
 ]),
 (1, "Kiểm tra đầu vào — Toán (Nâng cao)", [
   ("Tích phân ∫2x dx bằng?", ["x² + C","2 + C","x²","2x² + C"], 0, 3),
   ("Giá trị lim(x→0) sin(x)/x là?", ["1","0","Vô cực","Không tồn tại"], 0, 3),
   ("Số nghiệm phân biệt của x³ - 3x + 2 = 0 là?", ["2","1","3","0"], 0, 4),
   ("Phương trình tiếp tuyến của y = x² tại x = 1 là?", ["y = 2x - 1","y = 2x + 1","y = x - 1","y = 2x"], 0, 4),
   ("Giá trị lớn nhất của y = -x² + 4x - 1 là?", ["3","4","1","-1"], 0, 4),
   ("Tổ hợp C(6,2) bằng?", ["15","12","30","20"], 0, 3),
   ("Đạo hàm của y = ln x là?", ["1/x","ln x","x","eˣ"], 0, 3),
   ("Tổng cấp số nhân lùi vô hạn 1 + 1/2 + 1/4 + ... bằng?", ["2","1","Vô cực","1.5"], 0, 4),
 ]),
 (2, "Kiểm tra đầu vào — Vật lý", [
   ("Đơn vị của công suất là?", ["Watt","Joule","Newton","Pascal"], 0, 1),
   ("Theo định luật II Newton, gia tốc a bằng?", ["F/m","m/F","F·m","F + m"], 0, 2),
   ("Tốc độ ánh sáng trong chân không xấp xỉ?", ["3×10⁸ m/s","3×10⁶ m/s","340 m/s","9,8 m/s"], 0, 1),
   ("Chu kì con lắc đơn phụ thuộc vào?", ["Chiều dài và gia tốc trọng trường","Khối lượng vật","Biên độ dao động","Vận tốc ban đầu"], 0, 3),
   ("Công thức tính động năng là?", ["½mv²","mgh","mv","½kx²"], 0, 2),
   ("Đơn vị của điện trở là?", ["Ôm (Ω)","Vôn","Ampe","Fara"], 0, 1),
   ("Vật rơi tự do (g = 10 m/s²), sau 3s vận tốc là?", ["30 m/s","10 m/s","90 m/s","3 m/s"], 0, 3),
   ("Hiện tượng giao thoa chứng tỏ ánh sáng có tính chất?", ["Sóng","Hạt","Điện","Từ"], 0, 4),
 ]),
 (3, "Kiểm tra đầu vào — Tiếng Anh", [
   ("She _____ to school every day.", ["goes","go","going","gone"], 0, 1),
   ("I have lived here _____ 2010.", ["since","for","from","at"], 0, 2),
   ("Choose the synonym of 'big'.", ["large","small","tiny","short"], 0, 2),
   ("If I _____ you, I would study harder.", ["were","am","was","will be"], 0, 4),
   ("The book _____ I read was interesting.", ["that","who","where","when"], 0, 2),
   ("Choose the past tense of 'go'.", ["went","gone","goed","going"], 0, 1),
   ("'Make a decision' means to _____.", ["decide","delay","refuse","forget"], 0, 3),
   ("He is interested _____ music.", ["in","on","at","of"], 0, 2),
 ]),
 ('CHEM', "Kiểm tra đầu vào — Hóa học", [
   ("Kí hiệu hóa học của nguyên tố Natri là?", ["Na","N","So","Nu"], 0, 1),
   ("Công thức hóa học của nước là?", ["H₂O","CO₂","O₂","H₂"], 0, 1),
   ("Nguyên tử Cacbon (Z=6) có bao nhiêu proton?", ["6","12","8","4"], 0, 2),
   ("Dung dịch trung tính có pH bằng?", ["7","0","14","1"], 0, 2),
   ("Khí nào là tác nhân chính gây hiệu ứng nhà kính?", ["CO₂","O₂","N₂","H₂"], 0, 2),
   ("Kim loại nào nhẹ nhất trong các kim loại sau?", ["Liti","Sắt","Vàng","Đồng"], 0, 3),
   ("Phản ứng tỏa nhiệt là phản ứng?", ["Giải phóng nhiệt ra môi trường","Thu nhiệt từ môi trường","Không thay đổi nhiệt","Hấp thụ ánh sáng"], 0, 3),
   ("Hóa trị của Oxi trong hợp chất H₂O là?", ["II","I","III","IV"], 0, 4),
 ]),
 ('LIT', "Kiểm tra đầu vào — Ngữ văn", [
   ("Tác phẩm 'Truyện Kiều' là của tác giả nào?", ["Nguyễn Du","Hồ Xuân Hương","Nguyễn Trãi","Tố Hữu"], 0, 1),
   ("'Truyện Kiều' được viết theo thể thơ nào?", ["Lục bát","Song thất lục bát","Thất ngôn bát cú","Thơ tự do"], 0, 2),
   ("'Mặt trời xuống biển như hòn lửa' sử dụng biện pháp tu từ nào?", ["So sánh","Ẩn dụ","Nhân hóa","Hoán dụ"], 0, 3),
   ("Tác phẩm 'Vợ nhặt' là của nhà văn nào?", ["Kim Lân","Nam Cao","Ngô Tất Tố","Vũ Trọng Phụng"], 0, 2),
   ("Bài thơ 'Tây Tiến' là của nhà thơ nào?", ["Quang Dũng","Tố Hữu","Xuân Diệu","Chế Lan Viên"], 0, 2),
   ("Phương thức biểu đạt chính của văn nghị luận là?", ["Nghị luận","Tự sự","Miêu tả","Biểu cảm"], 0, 3),
   ("Bài thơ 'Sóng' là của nữ sĩ nào?", ["Xuân Quỳnh","Hồ Xuân Hương","Anh Thơ","Bà Huyện Thanh Quan"], 0, 2),
   ("Nhân vật Chí Phèo xuất hiện trong tác phẩm của?", ["Nam Cao","Kim Lân","Ngô Tất Tố","Vũ Trọng Phụng"], 0, 4),
 ]),
]

def q(s):
    return s.replace("'", "''")

out = []
out.append("/* Tự động sinh từ gen_question_bank.py — Mở rộng kho đề Kiểm tra đầu vào */")
out.append("USE [PrepAce];\nGO\nSET NOCOUNT ON;\nGO\n")

# Đảm bảo cột tồn tại
out.append("""IF COL_LENGTH('dbo.Quizzes','quiz_type') IS NULL ALTER TABLE dbo.Quizzes ADD quiz_type NVARCHAR(50) NULL;
IF COL_LENGTH('dbo.Questions','cognitive_level') IS NULL ALTER TABLE dbo.Questions ADD cognitive_level INT NULL;
GO""")

# Tạo thêm course Hóa học, Ngữ văn (idempotent theo tiêu đề)
out.append("""
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
GO""")

# Xóa toàn bộ dữ liệu ENTRY_TEST cũ
out.append("""
DELETE o FROM dbo.QuestionOptions o JOIN dbo.Questions q ON o.question_id=q.question_id JOIN dbo.Quizzes z ON q.quiz_id=z.quiz_id WHERE z.quiz_type='ENTRY_TEST';
DELETE qa FROM dbo.QuizAttempts qa JOIN dbo.Quizzes z ON qa.quiz_id=z.quiz_id WHERE z.quiz_type='ENTRY_TEST';
DELETE qq FROM dbo.Questions qq JOIN dbo.Quizzes z ON qq.quiz_id=z.quiz_id WHERE z.quiz_type='ENTRY_TEST';
DELETE FROM dbo.Quizzes WHERE quiz_type='ENTRY_TEST';
GO""")

# Chèn các đề
out.append("\nDECLARE @chem INT, @lit INT, @qz INT, @qid INT;")
out.append("SELECT @chem = course_id FROM dbo.Courses WHERE course_title = N'Hóa học 12 - Luyện thi THPT QG';")
out.append("SELECT @lit  = course_id FROM dbo.Courses WHERE course_title = N'Ngữ văn 12 - Ôn luyện THPT QG';")

for course_ref, title, questions in TESTS:
    cref = {1:"1",2:"2",3:"3","CHEM":"@chem","LIT":"@lit"}[course_ref]
    out.append(f"\nINSERT INTO dbo.Quizzes (course_id, quiz_title, duration_minutes, created_at, quiz_type) VALUES ({cref}, N'{q(title)}', 20, GETDATE(), 'ENTRY_TEST'); SET @qz = SCOPE_IDENTITY();")
    for content, opts, ci, lvl in questions:
        correct = opts[ci]
        out.append(f"INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES (@qz, N'{q(content)}', N'{q(correct)}', NULL, {lvl}); SET @qid = SCOPE_IDENTITY();")
        vals = ",".join(f"(@qid,N'{q(o)}')" for o in opts)
        out.append(f"INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES {vals};")

out.append("\nGO\nPRINT '✅ Mở rộng kho đề Kiểm tra đầu vào hoàn tất.';\nGO")

with open("D:/SWP391/PrepACE/sql/prepace_question_bank.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(out))
print("Generated prepace_question_bank.sql with", len(TESTS), "entry tests,",
      sum(len(t[2]) for t in TESTS), "questions")
