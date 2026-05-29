/**
 * File: LandingPage.jsx
 * Date: 2026-05-29
 * #by Kiri Team
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function LandingPage() {
    const navigate = useNavigate();
    const [fromCity, setFromCity] = useState('Pachora');
    const [toCity, setToCity] = useState('Jalgaon');

    const handleSearch = (e) => {
        e.preventDefault();
        navigate(`/map?from=${encodeURIComponent(fromCity)}&to=${encodeURIComponent(toCity)}`);
    };

    return (
        <div style={{
            position: 'relative',
            overflow: 'hidden',
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'var(--bg-main)'
        }}>
            {/* Background ambient glows */}
            <div style={{ position: 'absolute', top: '-10%', left: '-5%', width: '50vw', height: '50vw', background: 'radial-gradient(circle, rgba(249,115,22,0.12) 0%, transparent 70%)', filter: 'blur(60px)', zIndex: 0, borderRadius: '50%', pointerEvents: 'none' }} />
            <div style={{ position: 'absolute', bottom: '-20%', right: '-10%', width: '60vw', height: '60vw', background: 'radial-gradient(circle, rgba(59,130,246,0.08) 0%, transparent 70%)', filter: 'blur(60px)', zIndex: 0, borderRadius: '50%', pointerEvents: 'none' }} />
            {/* Top Navigation */}
            <header style={{
                padding: 'calc(var(--safe-top) + 12px) 16px 12px 16px',
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 8,
                backgroundColor: 'transparent',
                zIndex: 10
            }}>
                <button
                    onClick={() => navigate('/driver')}
                    className="btn btn-secondary btn-sm"
                    style={{ padding: '0 10px', fontSize: '12px' }}
                >
                    👨‍✈️ Driver
                </button>
                <button
                    onClick={() => navigate('/admin')}
                    className="btn btn-secondary btn-sm"
                    style={{ padding: '0 10px', fontSize: '12px' }}
                >
                    🛡️ Admin
                </button>
            </header>

            {/* Hero Section */}
            <main style={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '0 24px'
            }}>
                <div style={{
                    maxWidth: 480,
                    width: '100%',
                    textAlign: 'center',
                    animation: 'fadeIn 0.5s ease',
                    position: 'relative',
                    zIndex: 1
                }}>
                    <div style={{ fontSize: 52, marginBottom: 16, animation: 'bounce 2s infinite' }}>
                        🚌
                    </div>
                    <h1 style={{
                        fontSize: '2.2rem',
                        fontWeight: 800,
                        color: '#fff',
                        marginBottom: 8,
                        letterSpacing: '-0.02em'
                    }}>
                        Travel Tracker
                    </h1>
                    <p style={{
                        fontSize: '1rem',
                        color: 'var(--text-muted)',
                        marginBottom: 32,
                        lineHeight: 1.5
                    }}>
                        Real-time tracking for Pachora to Jalgaon buses.
                    </p>

                    {/* Compact Search Card */}
                    <div className="compact-search-card">
                        <form onSubmit={handleSearch}>
                            <div className="search-inputs-wrapper">
                                <div className="search-input-row">
                                    <div className="search-indicator">
                                        <div className="dot origin"></div>
                                        <div className="line-connector"></div>
                                    </div>
                                    <input
                                        className="compact-input-field"
                                        value={fromCity}
                                        onChange={(e) => setFromCity(e.target.value)}
                                        placeholder="From Station"
                                        required
                                    />
                                </div>

                                <button
                                    type="button"
                                    onClick={() => { const temp = fromCity; setFromCity(toCity); setToCity(temp); }}
                                    className="floating-swap-btn"
                                >
                                    ⇅
                                </button>

                                <div className="search-input-row">
                                    <div className="search-indicator">
                                        <div className="dot destination"></div>
                                    </div>
                                    <input
                                        className="compact-input-field"
                                        value={toCity}
                                        onChange={(e) => setToCity(e.target.value)}
                                        placeholder="To Station"
                                        required
                                    />
                                </div>
                            </div>

                            <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
                                <button
                                    type="button"
                                    onClick={() => navigate(`/map?from=${encodeURIComponent(fromCity)}&to=${encodeURIComponent(toCity)}&view=route`)}
                                    className="btn btn-primary"
                                    style={{
                                        flex: 1,
                                        height: 52,
                                        borderRadius: 12,
                                        justifyContent: 'center',
                                        fontSize: 15,
                                        boxShadow: '0 4px 12px rgba(249,115,22,0.3)',
                                        background: 'linear-gradient(135deg, #f97316 0%, #ea580c 100%)'
                                    }}
                                >
                                    View Route
                                </button>
                                <button
                                    type="button"
                                    onClick={() => navigate(`/map?from=${encodeURIComponent(fromCity)}&to=${encodeURIComponent(toCity)}&view=map`)}
                                    className="btn btn-secondary"
                                    style={{
                                        flex: 1,
                                        height: 52,
                                        borderRadius: 12,
                                        justifyContent: 'center',
                                        fontSize: 15,
                                        border: '1px solid var(--border)'
                                    }}
                                >
                                    View Map
                                </button>
                            </div>
                        </form>

                        {/* Search History Section */}
                        <div className="search-history-section">
                            <span className="history-label">Search History</span>
                            <div className="history-list">
                                {[
                                    { f: 'Pachora', t: 'Jalgaon' },
                                    { f: 'Jalgaon', t: 'Pachora' }
                                ].map((route, i) => (
                                    <div
                                        key={i}
                                        className="history-item"
                                        onClick={() => {
                                            setFromCity(route.f);
                                            setToCity(route.t);
                                            navigate(`/map?from=${encodeURIComponent(route.f)}&to=${encodeURIComponent(route.t)}`);
                                        }}
                                    >
                                        <div className="history-route">
                                            <span>{route.f}</span>
                                            <span className="history-arrow">→</span>
                                            <span>{route.t}</span>
                                        </div>
                                        <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>›</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            {/* Removing old unused styles */}
            <style dangerouslySetInnerHTML={{
                __html: `
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                @keyframes bounce {
                    0%, 100% { transform: translateY(0); }
                    50% { transform: translateY(-8px); }
                }
            `}} />
        </div>
    );
}
