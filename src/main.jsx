/**
 * File: main.jsx
 * Date: 2026-05-29
 * #by Kiri Team
 */
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </React.StrictMode>
);
\n\n/**
 * React Entry Point
 * Bootstraps the React application and mounts it to the DOM.
 * Wraps the root App component in a BrowserRouter for routing capabilities.
 */