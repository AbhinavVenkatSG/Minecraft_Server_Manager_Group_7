import React, { useState } from 'react';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Dashboard from './views/Dashboard';
import Console from './views/Console';
import FileEditor from './views/FileEditor';

function App() {
    // Navigation state to switch between views
    const [activeTab, setActiveTab] = useState('dashboard');

    // Server operational state as per REQ-SVR-001 (STARTUP, STOPPED, RUNNING, BLOCKED)
    const [serverStatus, setServerStatus] = useState('STOPPED');

    // Error state for REQ-USE-030 status display
    const [error, setError] = useState(null);

    // Component router logic
    const renderView = () => {
        switch (activeTab) {
            case 'dashboard':
                return <Dashboard />;
            case 'console':
                return <Console />;
            case 'files':
                return <FileEditor />;
            default:
                return <Dashboard />;
        }
    };

    return (
        <div className="flex h-screen bg-slate-950 text-white font-sans overflow-hidden">
            {/* Sidebar: Handles grouped menu (REQ-USE-010) and hover tooltips (REQ-USE-020) */}
            <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

            <div className="flex-1 flex flex-col min-w-0">
                {/* Header: Displays persistent connection status (REQ-USE-030) */}
                <Header status={serverStatus} errorMessage={error} />

                {/* Main Content Area */}
                <main className="flex-1 overflow-y-auto p-8 bg-slate-900/50">
                    <div className="max-w-6xl mx-auto">
                        {renderView()}
                    </div>
                </main>
            </div>
        </div>
    );
}

export default App;