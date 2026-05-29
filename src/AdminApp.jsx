/**
 * File: AdminApp.jsx
 * Date: 2026-05-29
 * #by Kiri Team
 */
import { Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { ref, get } from 'firebase/database';
import { auth, db } from './firebase';
import AdminLogin from './pages/AdminLogin';
import Dashboard from './pages/Dashboard';
import AddTravel from './pages/AddTravel';
import ManageTravels from './pages/ManageTravels';
import Sidebar from './components/Sidebar';

export default function AdminApp() {
    const [user, setUser] = useState(null);
    const [isAdmin, setIsAdmin] = useState(false);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const unsub = onAuthStateChanged(auth, async (u) => {
            try {
                if (u) {
                    // Check if user is an admin (using singular 'admin/' as per original code)
                    const snap = await get(ref(db, `admin/${u.uid}`));
                    if (snap.exists()) {
                        setUser(u);
                        setIsAdmin(true);
                    } else {
                        // Not an admin in DB, sign out to prevent session conflict
                        console.warn("Uid is not in admin node, signing out");
                        await auth.signOut();
                        setUser(null);
                        setIsAdmin(false);
                    }
                } else {
                    setUser(null);
                    setIsAdmin(false);
                }
            } catch (error) {
                console.error("Admin check failed:", error);
                setUser(null);
                setIsAdmin(false);
            } finally {
                setLoading(false);
            }
        });
        return unsub;
    }, []);

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', flexDirection: 'column', gap: 16 }}>
            <div className="spinner" />
            <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>Loading admin panel...</p>
        </div>
    );

    if (!user || !isAdmin) return <AdminLogin />;

    return (
        <div className="admin-layout">
            <Sidebar user={user} />
            <main className="admin-main-content">
                <Routes>
                    {/* Admin Dashboard: Overview of active travels and system stats */}
                    <Route path="/" element={<Dashboard />} />
                    {/* Add Travel: Form to create and assign new trips to drivers */}
                    <Route path="/add-travel" element={<AddTravel />} />
                    {/* Manage Travels: View, edit, or delete existing routes and drivers */}
                    <Route path="/manage" element={<ManageTravels />} />
                    {/* Fallback route: Redirects any unknown admin routes back to the dashboard */}
                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </main>
        </div>
    );
}
\n\n/**
 * AdminApp Module Root
 * Acts as a layout and route protector for the entire Admin section.
 * Enforces admin authentication state before rendering nested admin routes.
 */