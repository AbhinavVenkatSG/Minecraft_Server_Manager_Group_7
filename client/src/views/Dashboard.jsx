import React, { useState, useEffect} from 'react';

const Dashboard = () => {
    const [telemetry, setTelemetry] = useState({
        cpu: 0,
        ram: 0,
        storage: 0,
        players: 0
    });
    return (
        <div className = "dashboard-container">
            <h1 className = "text-2xl font-bold mb-6"> Server Telemetry</h1>

            {/* Grouped Telemetry*/}
            <div className = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className = "p-4 bg-gray-800 rounded-lg shadow" title = "Current CPU load">
                    <h3 className = "tect-gray-400 tex-sm">CPU usage</h3>
                    <p className = "text-3xl font-mono">{telemetry.cpu}%</p>
                </div>

                <div className="p-4 bg-gray-800 rounded-lg shadow" title="Memory Allocation">
                    <h3 className="text-gray-400 text-sm">RAM Usage</h3>
                    <p className="text-3xl font-mono">{telemetry.ram} GB</p>
                </div>

                <div className="p-4 bg-gray-800 rounded-lg shadow" title="Disk Space Used">
                    <h3 className="text-gray-400 text-sm">Storage</h3>
                    <p className="text-3xl font-mono">{telemetry.storage}%</p>
                </div>

                <div className="p-4 bg-gray-800 rounded-lg shadow" title="Active Players">
                    <h3 className="text-gray-400 text-sm">Players</h3>
                    <p className="text-3xl font-mono">{telemetry.players}</p>
                </div>
            </div>

            <div className = "mt-8 p-6 bg-gray-800 rounded-lg">
                <h2 className = "text-xl md-4">Quick State</h2>
                <p className = "text-gray-400"> Waiting for 1Hz telemetry update...</p>
            </div>
        </div>
    );
};

export default Dashboard;
