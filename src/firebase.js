/**
 * File: firebase.js
 * Date: 2026-05-29
 * #by Kiri Team
 */
// IMPORTANT: Replace these with your actual Firebase project config
// Get from: https://console.firebase.google.com → Project Settings → Your Apps → Firebase SDK snippet

import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getDatabase } from 'firebase/database';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
    apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "",
    authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "pachora-jalgaon-tracker.firebaseapp.com",
    databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL || "https://pachora-jalgaon-tracker-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "pachora-jalgaon-tracker",
    storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "pachora-jalgaon-tracker.firebasestorage.app",
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "934921741539",
    appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:934921741539:web:6e9dd157721059d097d1d6",
    measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID || "G-CC76V337PC"
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getDatabase(app);
export const storage = getStorage(app);

// Secondary app to create driver accounts without logging out admin
export const secondaryApp = initializeApp(firebaseConfig, "Secondary");
export const secondaryAuth = getAuth(secondaryApp);

export default app;
\n\n/**
 * Firebase Configuration
 * Initializes the Firebase app with project credentials.
 * Exports reusable `auth` and `db` (Realtime Database) instances for the app.
 */