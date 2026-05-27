import { NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase';
import { useState } from 'react';

const navItems = [
    { path: '/admin', label: 'Dashboard', icon: '📊' },
    { path: '/admin/add-travel', label: 'Add Travel', icon: '➕' },
    { path: '/admin/manage', label: 'Manage Travels', icon: '🚌' },
];

export default function Sidebar({ user }) {
    const navigate = useNavigate();

    const handleLogout = async () => {
        await signOut(auth);
        navigate('/');
    };

    return (
        <>
            {/* Desktop Sidebar */}
            <aside className="desktop-sidebar">
                {/* Logo */}
                <div style={{
                    padding: '24px 20px 20px',
                    borderBottom: '1px solid var(--border)',
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <span style={{ fontSize: 28 }}>🚌</span>
                        <div>
                            <div style={{ fontWeight: 800, fontSize: 13, color: 'var(--primary)', lineHeight: 1.2 }}>PACHORA–JALGAON</div>
                            <div style={{ fontSize: 10, color: 'var(--text-muted)', fontWeight: 500 }}>TRAVEL TRACKER</div>
                        </div>
                    </div>
                    <div style={{
                        marginTop: 12,
                        padding: '12px',
                        background: 'var(--bg-card2)',
                        borderRadius: 12,
                        fontSize: 12,
                        color: 'var(--text-muted)',
                        border: '1px solid var(--border)'
                    }}>
                        <span style={{ display: 'block', fontWeight: 600, color: 'var(--text)', fontSize: 13 }}>Admin Panel</span>
                        <span style={{ wordBreak: 'break-all', fontSize: 11, opacity: 0.8 }}>{user?.email}</span>
                    </div>
                </div>

                {/* Navigation */}
                <nav style={{ flex: 1, padding: '16px 12px' }}>
                    {navItems.map(item => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            end={item.path === '/'}
                            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                        >
                            <span className="icon">{item.icon}</span>
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                {/* Logout */}
                <div style={{ padding: '20px 16px', borderTop: '1px solid var(--border)' }}>
                    <button
                        className="btn btn-danger"
                        style={{ width: '100%' }}
                        onClick={handleLogout}
                    >
                        🚪 Logout
                    </button>
                </div>
            </aside>

            {/* Mobile Bottom Navigation */}
            <nav className="mobile-bottom-nav">
                {navItems.map(item => (
                    <NavLink
                        key={item.path}
                        to={item.path}
                        end={item.path === '/'}
                        className={({ isActive }) => `mobile-nav-item ${isActive ? 'active' : ''}`}
                    >
                        <span className="icon">{item.icon}</span>
                        <span className="label">{item.label}</span>
                    </NavLink>
                ))}
                <button className="mobile-nav-item" onClick={handleLogout} style={{ background: 'transparent', border: 'none' }}>
                    <span className="icon">🚪</span>
                    <span className="label">Logout</span>
                </button>
            </nav>
        </>
    );
}
