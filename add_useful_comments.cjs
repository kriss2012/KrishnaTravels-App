const fs = require('fs');
const path = require('path');

const srcDir = 'D:\\all files\\New folder\\KrishnaTravels-App\\src';

const fileComments = {
    'QRCodeGenerator.jsx': `/**\n * QRCodeGenerator Component\n * Generates a scannable QR code for a specific travel route.\n * Passengers can scan this to quickly access the live tracking map.\n */`,
    'Sidebar.jsx': `/**\n * Sidebar Component\n * Provides navigation for the Admin dashboard.\n * Includes links to the main dashboard, adding new travels, and managing existing ones.\n */`,
    'AddTravel.jsx': `/**\n * AddTravel Page\n * Admin interface for creating a new travel instance.\n * Allows admins to input route details, assign a driver, and initialize tracking.\n */`,
    'AdminLogin.jsx': `/**\n * AdminLogin Page\n * Handles authentication for administrators.\n * Verifies credentials against Firebase Auth before granting access to the admin panel.\n */`,
    'Dashboard.jsx': `/**\n * Admin Dashboard Page\n * The main landing view for admins after logging in.\n * Displays high-level analytics, active travels, and quick actions.\n */`,
    'DriverDashboard.jsx': `/**\n * DriverDashboard Page\n * The primary operational interface for drivers.\n * Responsible for fetching the assigned route and streaming live GPS coordinates to Firebase.\n */`,
    'DriverLogin.jsx': `/**\n * DriverLogin Page\n * Handles authentication for drivers.\n * Ensures only authorized drivers can access the location streaming dashboard.\n */`,
    'LandingPage.jsx': `/**\n * LandingPage Component\n * The public entry point of the Krishna Travels application.\n * Provides navigation to public tracking, or login portals for staff.\n */`,
    'ManageTravels.jsx': `/**\n * ManageTravels Page\n * Admin interface for overseeing all active and past routes.\n * Allows admins to edit details, reassign drivers, or terminate trips.\n */`,
    'PublicMap.jsx': `/**\n * PublicMap Page\n * The consumer-facing tracking interface.\n * Subscribes to real-time Firebase location data to display the bus/vehicle on an interactive map.\n */`,
    'AdminApp.jsx': `/**\n * AdminApp Module Root\n * Acts as a layout and route protector for the entire Admin section.\n * Enforces admin authentication state before rendering nested admin routes.\n */`,
    'App.jsx': `/**\n * Main Application Router\n * The root routing component that splits the application into three main domains:\n * 1. Public Pages (Landing, Tracking Map)\n * 2. Driver Module (/driver/*)\n * 3. Admin Module (/admin/*)\n */`,
    'DriverApp.jsx': `/**\n * DriverApp Module Root\n * Acts as a layout and route protector for the Driver section.\n * Verifies driver session state before rendering the live tracking dashboard.\n */`,
    'firebase.js': `/**\n * Firebase Configuration\n * Initializes the Firebase app with project credentials.\n * Exports reusable \`auth\` and \`db\` (Realtime Database) instances for the app.\n */`,
    'main.jsx': `/**\n * React Entry Point\n * Bootstraps the React application and mounts it to the DOM.\n * Wraps the root App component in a BrowserRouter for routing capabilities.\n */`
};

function walkSync(dir, callback) {
  if (!fs.existsSync(dir)) return;
  const stat = fs.statSync(dir);
  if (!stat.isDirectory()) return;
  fs.readdirSync(dir).forEach(file => {
    let fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      walkSync(fullPath, callback);
    } else {
      callback(fullPath);
    }
  });
}

walkSync(srcDir, (filePath) => {
  if (filePath.endsWith('.jsx') || filePath.endsWith('.js')) {
    const fileName = path.basename(filePath);
    const commentToAdd = fileComments[fileName];
    
    if (commentToAdd) {
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Don't add if already present
        if (!content.includes(commentToAdd.split('\\n')[1])) { // checking the second line of the comment
            // Find a good place to insert. After the top `/** ... */` block and imports.
            // A simple way is to insert it right before `export default` or `export function` or `function` or `const [A-Z]`
            
            // Let's just place it right after all import statements.
            const lines = content.split('\\n');
            let lastImportIndex = -1;
            for (let i = 0; i < lines.length; i++) {
                if (lines[i].trim().startsWith('import ')) {
                    lastImportIndex = i;
                }
            }
            
            if (lastImportIndex !== -1) {
                lines.splice(lastImportIndex + 1, 0, '\\n' + commentToAdd);
            } else {
                // if no imports, put it after the initial header comment block if it exists
                let insertAt = 0;
                if (lines[0].startsWith('/**')) {
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].includes('*/')) {
                            insertAt = i + 1;
                            break;
                        }
                    }
                }
                lines.splice(insertAt, 0, '\\n' + commentToAdd);
            }
            
            fs.writeFileSync(filePath, lines.join('\\n'), 'utf8');
            console.log(`Added useful component doc-comment to: \${fileName}`);
        }
    }
  }
});
console.log("Done adding functional documentation comments.");
