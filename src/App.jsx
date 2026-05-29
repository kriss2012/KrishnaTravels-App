/**
 * File: App.jsx
 * Date: 2026-05-29
 * #by Kiri Team
 */
import { Routes, Route } from 'react-router-dom';
import AdminApp from './AdminApp';
import DriverApp from './DriverApp';
import PublicMap from './pages/PublicMap';
import LandingPage from './pages/LandingPage';
import './index.css';

function App() {
    return (
        <Routes>
            {/* Landing page for the application */}
            <Route path="/" element={<LandingPage />} />
            {/* Public map view for live tracking */}
            <Route path="/map" element={<PublicMap />} />
            {/* Driver module - handles all /driver/* nested routes (requires driver authentication) */}
            <Route path="/driver/*" element={<DriverApp />} />
            {/* Admin module - handles all /admin/* nested routes (requires admin authentication) */}
            <Route path="/admin/*" element={<AdminApp />} />
        </Routes>
    );
}

export default App;
\n\n/**
 * Main Application Router
 * The root routing component that splits the application into three main domains:
 * 1. Public Pages (Landing, Tracking Map)
 * 2. Driver Module (/driver/*)
 * 3. Admin Module (/admin/*)
 */