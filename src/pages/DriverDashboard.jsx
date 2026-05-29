/**
 * File: DriverDashboard.jsx
 * Date: 2026-05-29
 * #by Kiri Team
 */
import { useState, useEffect, useRef } from 'react';
import { ref, get, update, onDisconnect } from 'firebase/database';
import { auth, db } from '../firebase';
import { signOut } from 'firebase/auth';

export default function DriverDashboard({ user }) {
    const [driverInfo, setDriverInfo] = useState(null);
    const [isOnline, setIsOnline] = useState(false);
    const [statusText, setStatusText] = useState('Fetching details...');
    const [loading, setLoading] = useState(true);
    const watchIdRef = useRef(null);
    const wakeLockRef = useRef(null);
    const intervalRef = useRef(null);

    useEffect(() => {
        const fetchDetails = async () => {
            try {
                const snap = await get(ref(db, `drivers/${user.uid}`));
                if (snap.exists()) {
                    setDriverInfo(snap.val());
                    setStatusText('Offline');
                } else {
                    setStatusText('Driver account not found.');
                }
            } catch (error) {
                setStatusText('Error fetching details.');
            }
            setLoading(false);
        };
        fetchDetails();

        // Register window events for clean shutdown
        const handleBeforeUnload = () => {
            if (isOnline) {
                // Best-effort manual trigger for browser close
                const travelsRef = ref(db, `travels/${driverInfo?.travelId}`);
                update(travelsRef, { isOnline: false, lastSeen: Date.now() });
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
            stopTracking();
        };
    }, [user.uid, isOnline, driverInfo?.travelId]);

    // Wake Lock functions
    const requestWakeLock = async () => {
        if ('wakeLock' in navigator) {
            try {
                wakeLockRef.current = await navigator.wakeLock.request('screen');

                wakeLockRef.current.addEventListener('release', () => {
                    if (isOnline) requestWakeLock();
                });
            } catch (err) {
                console.warn(`Wake Lock Error: ${err.message}`);
            }
        }
    };

    const releaseWakeLock = () => {
        if (wakeLockRef.current !== null) {
            wakeLockRef.current.release();
            wakeLockRef.current = null;
        }
    };

    const updateLocation = () => {
        if (!driverInfo || !driverInfo.travelId) return;

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const { latitude, longitude, speed } = position.coords;
                try {
                    const travelsRef = ref(db, `travels/${driverInfo.travelId}`);

                    // Setup onDisconnect ONLY once when going online
                    // But we re-verify it's active here
                    onDisconnect(travelsRef).update({
                        isOnline: false,
                        lastSeen: Date.now()
                    });

                    await update(travelsRef, {
                        isOnline: true,
                        lastSeen: Date.now(),
                        location: {
                            latitude,
                            longitude,
                            speed: speed || 0,
                            timestamp: Date.now()
                        }
                    });
                    setStatusText('🟢 Sharing Live Location');
                } catch (err) {
                    setStatusText('Error updating location');
                }
            },
            (error) => {
                let msg = "GPS Error";
                if (error.code === 1) msg = "Location Access Denied";
                else if (error.code === 2) msg = "GPS Signal Lost";
                else if (error.code === 3) msg = "Timeout searching for GPS";
                setStatusText(`❌ ${msg}: ${error.message}`);
            },
            {
                enableHighAccuracy: true,
                maximumAge: 0,
                timeout: 3000 // Shorter timeout for faster retries
            }
        );
    };

    const startTracking = async () => {
        if (!navigator.geolocation) {
            alert('Geolocation is not supported by your browser.');
            return;
        }

        if (!driverInfo || !driverInfo.travelId) return;

        setStatusText('Starting 1s Stream...');

        try {
            if (navigator.permissions && navigator.permissions.query) {
                const result = await navigator.permissions.query({ name: 'geolocation' });
                if (result.state === 'denied') {
                    alert('Location access is denied. Please enable it in your Android settings.');
                    return;
                }
            }
        } catch (e) {
            console.warn("Permissions API failed", e);
        }

        setIsOnline(true);
        requestWakeLock();

        try {
            const travelsRef = ref(db, `travels/${driverInfo.travelId}`);

            // Set disconnect handler immediately
            onDisconnect(travelsRef).update({
                isOnline: false,
                lastSeen: Date.now()
            });

            await update(travelsRef, {
                isOnline: true,
                lastSeen: Date.now()
            });

            // Initial immediate update
            updateLocation();

            // Clear any existing interval
            if (intervalRef.current) clearInterval(intervalRef.current);

            // Set interval for every 1 second for ultra-fast tracking
            intervalRef.current = setInterval(() => {
                updateLocation();
            }, 1000);

            setStatusText('🟢 Sharing Live Location');
        } catch (err) {
            setStatusText('Error going online.');
            setIsOnline(false);
            releaseWakeLock();
        }
    };

    const stopTracking = async () => {
        setIsOnline(false);
        setStatusText('Offline');
        releaseWakeLock();

        if (intervalRef.current) {
            clearInterval(intervalRef.current);
            intervalRef.current = null;
        }

        if (driverInfo && driverInfo.travelId) {
            try {
                await update(ref(db, `travels/${driverInfo.travelId}`), {
                    isOnline: false,
                    lastSeen: Date.now()
                });
            } catch (e) {
                console.error("Failed to update status to offline");
            }
        }
    };

    const toggleStatus = () => {
        if (isOnline) {
            stopTracking();
        } else {
            startTracking();
        }
    };

    const handleLogout = async () => {
        await stopTracking();
        await signOut(auth);
    };

    if (loading) {
        return (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', backgroundColor: 'var(--bg-main)' }}>
                <div className="spinner" />
            </div>
        );
    }

    return (
        <div style={{
            minHeight: '100vh',
            backgroundColor: 'var(--bg-main)',
            display: 'flex',
            flexDirection: 'column',
        }}>
            <header style={{
                padding: '16px 24px',
                backgroundColor: 'var(--bg-card)',
                borderBottom: '1px solid var(--border)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ fontSize: 24 }}>👨‍✈️</div>
                    <div>
                        <h1 style={{ margin: 0, fontSize: 16, color: 'var(--text-main)' }}>{driverInfo?.name || 'Driver'}</h1>
                        <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>ID: {driverInfo?.travelId || 'N/A'}</p>
                    </div>
                </div>
                <button
                    onClick={handleLogout}
                    style={{ background: 'none', border: 'none', color: 'var(--red)', cursor: 'pointer', fontWeight: 600, fontSize: 14 }}
                >
                    Logout
                </button>
            </header>

            <main style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '16px 20px' }}>
                <div style={{
                    width: '100%',
                    maxWidth: 400,
                    backgroundColor: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderRadius: 24,
                    padding: '24px 20px',
                    textAlign: 'center',
                    boxShadow: isOnline ? '0 0 40px rgba(16, 185, 129, 0.15)' : 'none',
                    transition: 'all 0.3s ease'
                }}>
                    <div style={{
                        width: 100,
                        height: 100,
                        borderRadius: '50%',
                        backgroundColor: isOnline ? 'rgba(16, 185, 129, 0.1)' : 'rgba(100, 116, 139, 0.1)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        margin: '0 auto 24px auto',
                        border: `4px solid ${isOnline ? '#10b981' : '#64748b'}`,
                        transition: 'all 0.3s ease',
                        position: 'relative',
                        boxShadow: isOnline ? '0 0 20px rgba(16, 185, 129, 0.3)' : 'none',
                        animation: isOnline ? 'pulse 2s infinite' : 'none'
                    }}>
                        <span style={{ fontSize: 40 }}>{isOnline ? '🚌' : '😴'}</span>
                    </div>

                    <h2 style={{ fontSize: 20, margin: '0 0 8px 0', color: 'var(--text-main)' }}>
                        {statusText}
                    </h2>

                    <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '0 0 16px 0', lineHeight: 1.5 }}>
                        {isOnline ?
                            'Streaming location in real-time (1s refresh).' :
                            'Go online when you start your journey from Pachora to Jalgaon or vice-versa.'}
                    </p>

                    <div style={{ backgroundColor: 'rgba(245, 158, 11, 0.1)', color: '#b45309', padding: '12px', borderRadius: 8, fontSize: 11, marginBottom: 24, textAlign: 'left' }}>
                        <strong>🚀 Ultra-Fast Tracking:</strong> We are now streaming your location every 1 second for maximum accuracy.
                        <strong>🔋 Battery Note:</strong> Continuous GPS is active.
                    </div>

                    <button
                        onClick={toggleStatus}
                        style={{
                            width: '100%',
                            padding: '16px',
                            borderRadius: 100,
                            border: 'none',
                            backgroundColor: isOnline ? 'var(--red)' : 'var(--primary)',
                            fontSize: 18,
                            fontWeight: 700,
                            cursor: 'pointer',
                            color: '#fff',
                            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                            transition: 'all 0.2s',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 8
                        }}
                    >
                        {isOnline ? (
                            <><span>⏹️</span> Go Offline</>
                        ) : (
                            <><span>▶️</span> Go Online</>
                        )}
                    </button>
                    {isOnline && (
                        <p style={{ marginTop: 16, fontSize: 12, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                            <span className="spinner" style={{ width: 12, height: 12, borderTopColor: '#10b981' }}></span> Live updates running in background...
                        </p>
                    )}
                </div>
            </main>
        </div>
    );
}
