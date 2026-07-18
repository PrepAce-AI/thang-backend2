const fs = require('fs');
const path = require('path');

const subjects = [
    { src: 'math_0101.json', prefix: 'math' },
    { src: 'physics_0214.json', prefix: 'physics' },
    { src: 'english_1116.json', prefix: 'english' },
    { src: 'physics_0214.json', prefix: 'chemistry' }, // copy physics as chemistry template
    { src: 'physics_0214.json', prefix: 'biology' }    // copy physics as biology template
];

function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
}

function shiftNumbers(text, amount) {
    if (typeof text !== 'string') return text;
    // Replace small integers (1-99) slightly by adding amount
    return text.replace(/\b([1-9][0-9]?)\b/g, (match, p1) => {
        let num = parseInt(p1);
        return String(num + amount);
    });
}

subjects.forEach(sub => {
    const filePath = path.join(__dirname, sub.src);
    if (!fs.existsSync(filePath)) {
        console.log(`Skipping missing file: ${filePath}`);
        return;
    }
    const rawData = fs.readFileSync(filePath, 'utf8');
    const template = JSON.parse(rawData);

    // Create 5 variants
    for (let i = 1; i <= 5; i++) {
        // ID format: 101, 202, ... 505
        let variantId = `0${i}0${i}`; 

        let variantData = JSON.parse(JSON.stringify(template)); // deep clone
        
        // Slightly alter questions to make them look distinct
        variantData.forEach(q => {
            // Shift numbers by i
            q.questionContent = shiftNumbers(q.questionContent, i);
            
            if (sub.prefix === 'chemistry' || sub.prefix === 'biology') {
                q.questionContent = q.questionContent.replace(/Vật Lý|vật lý/gi, sub.prefix === 'chemistry' ? 'Hóa học' : 'Sinh học');
            }
            
            if (q.options) {
                q.options.forEach(opt => {
                    opt.optionContent = shiftNumbers(opt.optionContent, i);
                });
            }

            if (q.questionType === "CHOICE") {
                shuffleArray(q.options);
            }
        });

        // shuffle the questions order
        shuffleArray(variantData);

        const outName = `${sub.prefix}_${variantId}.json`;
        fs.writeFileSync(path.join(__dirname, outName), JSON.stringify(variantData, null, 2));
        console.log(`Created ${outName}`);
    }
});
