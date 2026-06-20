# -*- coding: utf-8 -*-
"""Sinh SQL nạp lại câu hỏi + đáp án cho các đề Luyện đề / Thi thử (quiz 1..9),
đảm bảo MỌI câu có 4 phương án và correct_answer khớp 1 phương án (chấm điểm đúng)."""

MATH = [
 ("Đạo hàm của hàm số y = x² là?", ["2x","x²","2","x"], 0),
 ("Đạo hàm của hàm số y = x³ là?", ["3x²","x²","3x","2x"], 0),
 ("Nguyên hàm của f(x) = 2x là?", ["x² + C","2 + C","x³ + C","2x² + C"], 0),
 ("log₂(8) bằng bao nhiêu?", ["3","2","4","8"], 0),
 ("Nghiệm của phương trình x² - 5x + 6 = 0 là?", ["x = 2 hoặc x = 3","x = 1 hoặc x = 6","x = -2 hoặc x = -3","Vô nghiệm"], 0),
 ("Giá trị lim(x→0) sin(x)/x là?", ["1","0","Vô cực","Không tồn tại"], 0),
 ("Tích phân ∫x² dx bằng?", ["x³/3 + C","2x + C","x³ + C","3x² + C"], 0),
 ("Giá trị nhỏ nhất của y = x² - 4x + 5 là?", ["1","0","5","-1"], 0),
 ("Tổ hợp C(5,2) bằng?", ["10","20","5","25"], 0),
 ("2¹⁰ bằng bao nhiêu?", ["1024","512","256","2048"], 0),
 ("Đạo hàm của y = ln x là?", ["1/x","ln x","x","eˣ"], 0),
 ("cos(60°) bằng bao nhiêu?", ["0.5","1","0","√3/2"], 0),
]
PHYS = [
 ("Đơn vị của lực trong hệ SI là?", ["Newton","Joule","Watt","Pascal"], 0),
 ("Theo định luật II Newton, gia tốc a bằng?", ["F/m","m/F","F·m","F + m"], 0),
 ("Đơn vị của công suất là?", ["Watt","Joule","Newton","Ampe"], 0),
 ("Công thức tính động năng là?", ["½mv²","mgh","mv","½kx²"], 0),
 ("Tốc độ ánh sáng trong chân không xấp xỉ?", ["3×10⁸ m/s","3×10⁶ m/s","340 m/s","9,8 m/s"], 0),
 ("Vật rơi tự do (g = 10 m/s²), sau 2s vận tốc là?", ["20 m/s","10 m/s","40 m/s","5 m/s"], 0),
 ("Đơn vị của điện trở là?", ["Ôm (Ω)","Vôn","Ampe","Fara"], 0),
 ("Chu kì con lắc đơn phụ thuộc vào?", ["Chiều dài và gia tốc trọng trường","Khối lượng vật","Biên độ dao động","Vận tốc ban đầu"], 0),
 ("Đại lượng nào sau đây là vô hướng?", ["Khối lượng","Lực","Vận tốc","Gia tốc"], 0),
 ("Công thức thế năng trọng trường là?", ["mgh","½mv²","mv","F·s"], 0),
]
ENG = [
 ("She _____ to school every day.", ["goes","go","going","gone"], 0),
 ("I have lived here _____ 2015.", ["since","for","from","at"], 0),
 ("Choose the synonym of 'happy'.", ["joyful","sad","angry","tired"], 0),
 ("If I _____ rich, I would travel the world.", ["were","am","was","will be"], 0),
 ("The book _____ I read was interesting.", ["that","who","where","when"], 0),
 ("Choose the past tense of 'go'.", ["went","gone","goed","going"], 0),
 ("He is interested _____ music.", ["in","on","at","of"], 0),
 ("Choose the correct sentence: 'She _____ TV now.'", ["is watching","watch","watches","watched"], 0),
 ("Choose the antonym of 'difficult'.", ["easy","hard","complex","tough"], 0),
 ("'A lot of' can be used with?", ["Cả danh từ đếm được và không đếm được","Chỉ danh từ đếm được","Chỉ danh từ không đếm được","Không từ nào"], 0),
]

# quiz_id -> (pool, slice)
ASSIGN = {
 1: (MATH, MATH[0:6]),
 2: (PHYS, PHYS[0:6]),
 3: (ENG,  ENG[0:6]),
 4: (MATH, MATH[2:10]),
 5: (MATH, MATH[0:10]),
 6: (PHYS, PHYS[0:8]),
 7: (PHYS, PHYS[2:10]),
 8: (ENG,  ENG[0:8]),
 9: (ENG,  ENG[2:10]),
}

def esc(s): return s.replace("'", "''")

out = ["/* Tự sinh từ gen_practice_bank.py — nạp lại câu hỏi+đáp án cho đề Luyện đề/Thi thử */",
       "USE [PrepAce];\nGO\nSET NOCOUNT ON;\nGO\n"]

ids = ",".join(str(i) for i in ASSIGN)
out.append(f"""/* Xóa an toàn theo thứ tự khóa ngoại (chỉ các quiz luyện đề/thi thử) */
DELETE sa FROM dbo.StudentAnswers sa JOIN dbo.Questions q ON sa.question_id=q.question_id WHERE q.quiz_id IN ({ids});
DELETE o  FROM dbo.QuestionOptions o JOIN dbo.Questions q ON o.question_id=q.question_id WHERE q.quiz_id IN ({ids});
DELETE FROM dbo.Questions WHERE quiz_id IN ({ids});
GO
""")

out.append("DECLARE @qid INT;")
for quiz_id, (pool, qs) in ASSIGN.items():
    out.append(f"\n/* ===== Quiz {quiz_id} ===== */")
    for content, opts, ci in qs:
        correct = opts[ci]
        lvl = ((abs(hash(content)) % 4) + 1)
        out.append(f"INSERT INTO dbo.Questions (quiz_id, question_content, correct_answer, explanation, cognitive_level) VALUES ({quiz_id}, N'{esc(content)}', N'{esc(correct)}', NULL, {lvl}); SET @qid=SCOPE_IDENTITY();")
        vals = ",".join(f"(@qid,N'{esc(o)}')" for o in opts)
        out.append(f"INSERT INTO dbo.QuestionOptions (question_id, option_content) VALUES {vals};")

out.append("\nGO\nPRINT '✅ Nạp lại đề Luyện đề/Thi thử hoàn tất.';\nGO")

with open("D:/SWP391/PrepACE/sql/prepace_practice_bank.sql","w",encoding="utf-8") as f:
    f.write("\n".join(out))
print("Generated. quizzes:", len(ASSIGN), "questions:", sum(len(q[1]) for q in ASSIGN.values()))
