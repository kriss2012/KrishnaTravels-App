const fs = require('fs');
const path = require('path');

const srcDir = 'D:\\all files\\New folder\\KrishnaTravels-App\\android';

function walkSync(dir, callback) {
  if (!fs.existsSync(dir)) return;
  const stat = fs.statSync(dir);
  if (!stat.isDirectory()) return;
  fs.readdirSync(dir).forEach(file => {
    if (file === 'build' || file === '.gradle') return;
    let fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      walkSync(fullPath, callback);
    } else {
      callback(fullPath);
    }
  });
}

const currentDate = new Date().toISOString().split('T')[0];

walkSync(srcDir, (filePath) => {
  if (filePath.endsWith('.kt') || filePath.endsWith('.java')) {
    let content = fs.readFileSync(filePath, 'utf8');
    const fileName = path.basename(filePath);
    
    // Check if it already has this exact comment
    if (content.includes(`* File: ${fileName}`) && content.includes(`* #by Kiri Team`)) {
        return;
    }

    const commentBlock = `/**\n * File: ${fileName}\n * Date: ${currentDate}\n * #by Kiri Team\n */\n`;

    fs.writeFileSync(filePath, commentBlock + content, 'utf8');
    console.log(`Added comment block to: ${filePath}`);
  }
});
console.log("Done updating comments for android files.");
