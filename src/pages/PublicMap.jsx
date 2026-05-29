/**
 * File: PublicMap.jsx
 * Date: 2026-05-29
 * #by Kiri Team
 */
import { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import 'leaflet/dist/leaflet.css';
import { ref, onValue } from 'firebase/database';
import { db } from '../firebase';
import L from 'leaflet';

// Fix leaflet default icon issue in React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Custom bus icon
const busIcon = new L.Icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/3204/3204098.png',
    iconSize: [38, 38],
    iconAnchor: [19, 38],
    popupAnchor: [0, -38]
});

export default function PublicMap() {
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const fromCity = queryParams.get('from') || 'Pachora';
    const toCity = queryParams.get('to') || 'Jalgaon';
    const viewMode = queryParams.get('view') || 'route'; // default to route list

    const [travels, setTravels] = useState({});
    const [selectedBusId, setSelectedBusId] = useState(null);
    const [userLocation, setUserLocation] = useState(null);
    const [isPanelVisible, setIsPanelVisible] = useState(true);

    const mapRef = useRef(null);
    const mapInstance = useRef(null);
    const markersRef = useRef({});
    const userMarkerRef = useRef(null);

    const defaultPosition = [20.75, 75.35];

    // Fetch Firebase Data
    useEffect(() => {
        const travelsRef = ref(db, 'travels');
        const unsub = onValue(travelsRef, (snapshot) => {
            if (snapshot.exists()) {
                setTravels(snapshot.val());
            } else {
                setTravels({});
            }
        });
        return () => unsub();
    }, []);

    // Initial view set based on URL
    useEffect(() => {
        if (viewMode === 'map') {
            setSelectedBusId(null);
        }
    }, [viewMode]);

    // Initialize Map
    useEffect(() => {
        if (!mapInstance.current && mapRef.current) {
            mapInstance.current = L.map(mapRef.current, {
                zoomControl: false
            }).setView(defaultPosition, 10);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap'
            }).addTo(mapInstance.current);

            L.control.zoom({ position: 'bottomright' }).addTo(mapInstance.current);
        }
    }, []);

    // My Location Logic
    const handleFindMe = () => {
        if (!navigator.geolocation) return;
        navigator.geolocation.getCurrentPosition((pos) => {
            const { latitude, longitude } = pos.coords;
            const latLng = [latitude, longitude];
            setUserLocation(latLng);

            if (mapInstance.current) {
                mapInstance.current.setView(latLng, 14);
                if (userMarkerRef.current) {
                    userMarkerRef.current.setLatLng(latLng);
                } else {
                    userMarkerRef.current = L.circleMarker(latLng, {
                        radius: 8, fillColor: '#3b82f6', color: 'white', weight: 2, opacity: 1, fillOpacity: 0.8
                    }).addTo(mapInstance.current).bindPopup('You are here');
                }
            }
        }, (err) => {
            alert("Please enable GPS for this feature.");
        }, { enableHighAccuracy: true });
    };

    // Filter logic
    const searchRoute1 = `${fromCity} → ${toCity}`;
    const allActiveBuses = Object.entries(travels)
        .map(([id, data]) => ({ id, ...data }))
        .filter(data => {
            if (!data.isOnline || !data.location) return false;
            const route = data.route || '';
            if (route.toLowerCase() === searchRoute1.toLowerCase()) return true;
            if (route.includes('Both ways') || route.includes('↔')) return true;
            if (!queryParams.get('from') && !queryParams.get('to')) return true;
            return false;
        });

    const hasAutoSelected = useRef(false);

    // Auto-select if ONLY one and in route view (Only on initial load/data)
    useEffect(() => {
        if (viewMode === 'route' && allActiveBuses.length === 1 && !selectedBusId && !hasAutoSelected.current) {
            setSelectedBusId(allActiveBuses[0].id);
            hasAutoSelected.current = true;
        }
    }, [allActiveBuses, selectedBusId, viewMode]);

    // Markers Sync
    useEffect(() => {
        if (!mapInstance.current) return;
        const activeIds = new Set(allActiveBuses.map(b => b.id));
        Object.keys(markersRef.current).forEach(id => {
            if (!activeIds.has(id)) {
                mapInstance.current.removeLayer(markersRef.current[id]);
                delete markersRef.current[id];
            }
        });
        allActiveBuses.forEach(data => {
            const { latitude, longitude, speed, timestamp } = data.location;
            const latLng = [latitude, longitude];
            const popupContent = `
                <div style="min-width: 140px; font-family: 'Inter', sans-serif;">
                    <h3 style="margin:0; font-size:15px; color:var(--primary);">${data.travelName}</h3>
                    <p style="margin:4px 0; font-size:11px; color:#64748b;">${speed ? Math.round(speed * 3.6) : 0} km/h • ${new Date(timestamp).toLocaleTimeString()}</p>
                </div>
            `;
            if (markersRef.current[data.id]) {
                markersRef.current[data.id].setLatLng(latLng);
                markersRef.current[data.id].getPopup().setContent(popupContent);
            } else {
                const marker = L.marker(latLng, { icon: busIcon }).bindPopup(popupContent);
                marker.addTo(mapInstance.current);
                markersRef.current[data.id] = marker;
            }
        });
    }, [travels, allActiveBuses]);

    const ROUTE_STOPS = [
        { name: 'Pachora', lat: 20.6681, lng: 75.3567, dist: '0 km' },
        { name: 'Goradkheda', lat: 20.6881, lng: 75.3681, dist: '3 km' },
        { name: 'Bildhi', lat: 20.7081, lng: 75.3800, dist: '6 km' },
        { name: 'Khedgaon', lat: 20.7281, lng: 75.3920, dist: '9 km' },
        { name: 'Hadsan', lat: 20.7481, lng: 75.4050, dist: '13 km' },
        { name: 'Nandra', lat: 20.7681, lng: 75.4180, dist: '16 km' },
        { name: 'Lasgaon', lat: 20.7881, lng: 75.4310, dist: '19 km' },
        { name: 'Samner', lat: 20.8081, lng: 75.4440, dist: '23 km' },
        { name: 'Pathri', lat: 20.8281, lng: 75.4570, dist: '26 km' },
        { name: 'Vadli', lat: 20.8481, lng: 75.4700, dist: '29 km' },
        { name: 'Wawadade', lat: 20.8681, lng: 75.4830, dist: '33 km' },
        { name: 'Ramdevwadi', lat: 20.8881, lng: 75.4960, dist: '36 km' },
        { name: 'Shirsoli', lat: 20.9081, lng: 75.5090, dist: '39 km' },
        { name: 'Jain', lat: 20.9281, lng: 75.5220, dist: '42 km' },
        { name: 'GH Raisoni', lat: 20.9481, lng: 75.5350, dist: '45 km' },
        { name: 'D Mart', lat: 20.9681, lng: 75.5480, dist: '48 km' },
        { name: 'Ichadevi', lat: 20.9881, lng: 75.5550, dist: '51 km' },
        { name: 'Jalgaon', lat: 21.0077, lng: 75.5626, dist: '52 km' }
    ];

    const getDistance = (lat1, lon1, lat2, lon2) => {
        const R = 6371; // km
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLon = (lon2 - lon1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    };

    const getTimelinePosition = (busLat, busLng) => {
        if (!busLat || !busLng) return 0;

        // Find which stops the bus is between (latitude based for Pachora -> Jalgaon)
        // Since it's Northwards, we find the first stop that has a higher lat than us
        let nextStopIndex = ROUTE_STOPS.findIndex(s => s.lat > busLat);

        if (nextStopIndex === -1) return 100; // Past Jalgaon
        if (nextStopIndex === 0) return 0;   // Before Pachora

        const prevStop = ROUTE_STOPS[nextStopIndex - 1];
        const nextStop = ROUTE_STOPS[nextStopIndex];

        // Linear interpolation within THIS specific segment
        const segmentProgress = (busLat - prevStop.lat) / (nextStop.lat - prevStop.lat);

        // Map segment progress to total timeline progress
        const stopsCount = ROUTE_STOPS.length;
        const baseProgressAcrossStops = ((nextStopIndex - 1) / (stopsCount - 1)) * 100;
        const segmentTotalWeight = (1 / (stopsCount - 1)) * 100;

        const finalProgress = baseProgressAcrossStops + (segmentProgress * segmentTotalWeight);
        return Math.min(Math.max(finalProgress, 0), 98);
    };

    const getNearestStop = (busLat, busLng) => {
        if (!busLat || !busLng) return null;
        let nearest = ROUTE_STOPS[0];
        let minDist = Infinity;
        ROUTE_STOPS.forEach(stop => {
            const d = getDistance(busLat, busLng, stop.lat, stop.lng);
            if (d < minDist) {
                minDist = d;
                nearest = stop;
            }
        });
        return { ...nearest, distanceAway: minDist };
    };

    const selectedBusData = allActiveBuses.find(b => b.id === selectedBusId);
    const nearestStop = selectedBusData?.location ? getNearestStop(selectedBusData.location.latitude, selectedBusData.location.longitude) : null;
    const timelinePos = selectedBusData?.location ? getTimelinePosition(selectedBusData.location.latitude, selectedBusData.location.longitude) : 0;

    return (
        <div style={{ height: '100vh', width: '100vw', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--bg-main)' }}>
            <header className="public-header" style={{
                padding: 'calc(var(--safe-top) + 12px) 16px 12px 16px',
                backgroundColor: 'var(--bg-card)',
                borderBottom: '1px solid var(--border)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                zIndex: 2005,
                gap: 12
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ width: 32, height: 32, backgroundColor: 'var(--primary)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 16 }}>
                        🚌
                    </div>
                    <div>
                        <h1 style={{ margin: 0, fontSize: 16, color: 'var(--text-main)' }}>Live Tracker</h1>
                        <p style={{ margin: 0, fontSize: 10, color: 'var(--text-muted)' }}>{fromCity} ⇄ {toCity}</p>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                    <button onClick={() => setIsPanelVisible(!isPanelVisible)} style={{ background: 'none', border: 'none', fontSize: 18, cursor: 'pointer' }}>
                        {isPanelVisible ? '🔽' : '🔼'}
                    </button>
                    <button
                        onClick={() => window.location.href = '/'}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '6px 12px', fontSize: '13px', fontWeight: 600 }}
                    >
                        ← Back
                    </button>
                </div>
            </header>

            <div style={{ flex: 1, position: 'relative' }}>
                <div ref={mapRef} style={{ height: '100%', width: '100%', zIndex: 1 }} />

                {/* Relocated Map Control */}
                <button
                    className="map-control-btn"
                    onClick={handleFindMe}
                    style={{ bottom: isPanelVisible && window.innerWidth < 768 ? '40vh' : '24px', transition: 'bottom 0.3s' }}
                >
                    🎯
                </button>

                {/* Professional Drawer Panel */}
                {isPanelVisible && (
                    <div className="spot-bus-panel">
                        <div className="drawer-drag-handle"></div>

                        {allActiveBuses.length === 0 ? (
                            <div style={{ padding: '40px 16px', textAlign: 'center' }}>
                                <div style={{ fontSize: 40, marginBottom: 12 }}>📡</div>
                                <h3 style={{ margin: 0, fontSize: 16, color: 'var(--text)' }}>No Travels Online</h3>
                                <p style={{ margin: '8px 0 0 0', fontSize: 13, color: 'var(--text-muted)' }}>Currently, there are no live buses sharing their location on this route.</p>
                                <button onClick={() => window.location.href = '/'} className="btn btn-primary btn-sm" style={{ marginTop: 24, padding: '8px 20px', borderRadius: '10px' }}>Try Another Route</button>
                            </div>
                        ) : !selectedBusId ? (
                            <div className="bus-list-container">
                                <h3 style={{ fontSize: 12, fontWeight: 800, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8, paddingLeft: 4 }}>Select a Bus</h3>
                                {allActiveBuses.map(bus => (
                                    <div key={bus.id} className="bus-selection-item" onClick={() => {
                                        setSelectedBusId(bus.id);
                                        if (mapInstance.current && bus.location) {
                                            mapInstance.current.setView([bus.location.latitude, bus.location.longitude], 14);
                                        }
                                    }}>
                                        <div className="bus-icon-circle">🚌</div>
                                        <div className="bus-details">
                                            <div className="bus-name-title">{bus.travelName}</div>
                                            <div className="bus-meta-info">
                                                <span>👤 {bus.driverName}</span>
                                                <span className="status-badge-live">Live</span>
                                            </div>
                                        </div>
                                        <div style={{ color: 'var(--primary)', opacity: 0.5 }}>›</div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 8px 8px 8px' }}>
                                    <button className="back-to-list-btn" onClick={() => setSelectedBusId(null)}>
                                        ‹ Back to List
                                    </button>
                                    <span className="status-badge-live" style={{ marginRight: 12 }}>Near {nearestStop?.name}</span>
                                </div>

                                <div className="route-timeline-container" style={{ padding: '12px 24px' }}>
                                    <div className="route-line-main" style={{ left: 37 }}></div>
                                    {selectedBusData?.location && (
                                        <div className="live-bus-indicator-timeline" style={{ left: 29, top: `${timelinePos}%` }}>
                                            🚌
                                        </div>
                                    )}
                                    {ROUTE_STOPS.map((stop, i) => {
                                        const isPassed = selectedBusData?.location?.latitude >= stop.lat - 0.001;
                                        const isNearest = nearestStop?.name === stop.name;

                                        return (
                                            <div key={i} className="route-stop-item" style={{ gap: 24 }}>
                                                <div
                                                    className={`stop-marker-dot ${isPassed ? 'active' : ''}`}
                                                    style={{
                                                        width: isNearest ? 12 : 10,
                                                        height: isNearest ? 12 : 10,
                                                        border: isNearest ? '3px solid var(--primary)' : '2px solid var(--border)',
                                                        background: 'var(--bg-card)',
                                                        boxShadow: isNearest ? '0 0 10px var(--primary)' : 'none'
                                                    }}
                                                ></div>
                                                <div className="stop-info">
                                                    <span className="stop-name" style={{ fontSize: 15, color: isNearest ? 'var(--primary)' : 'var(--text)', fontWeight: isNearest ? 800 : 500 }}>{stop.name}</span>
                                                    <span className="stop-distance">{stop.dist}</span>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>

                                <div style={{ padding: '20px', background: 'var(--bg-card2)', borderTop: '1px solid var(--border)' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                        <div>
                                            <h4 style={{ margin: 0, fontSize: 16, fontWeight: 800, color: 'var(--text)' }}>{selectedBusData?.travelName}</h4>
                                            <p style={{ margin: '4px 0 0 0', fontSize: 12, color: 'var(--text-muted)' }}>📍 Nearest: {nearestStop?.name} ({nearestStop?.distanceAway.toFixed(1)} km)</p>
                                        </div>
                                        <div style={{ textAlign: 'right' }}>
                                            <div style={{ fontSize: 18, fontWeight: 900, color: 'var(--primary)' }}>{Math.round((selectedBusData?.location?.speed || 0) * 3.6)} <span style={{ fontSize: 10, fontWeight: 600 }}>KM/H</span></div>
                                            <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>Live Speed</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
