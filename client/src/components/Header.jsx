import React from 'react';

const Header = ({ status, errorMessage }) => {
    // Helper to determine the color of the status indicator
    const getStatusColor = () => {
        switch (status) {
            case 'RUNNING': return 'bg-green-500';
            case 'STARTUP': return 'bg-yellow-500';
            case 'STOPPED': return 'bg-red-500';
            case 'BLOCKED': return 'bg-orange-600';
            default: return 'bg-gray-500';
        }
    };

    return (
        <header className="h-16 bg-gray-800 border-b border-gray-700 flex items-center justify-between px-6 shadow-md">
            <div className="flex items-center space-x-4">
                <h2 className="text-sm font-medium text-gray-400 uppercase tracking-widest">
                    Connection Status
                </h2>
                <div className="flex items-center space-x-2 bg-gray-900 px-3 py-1 rounded-full border border-gray-700">
                    {/* Real-time status indicator light */}
                    <span className={`h-2 w-2 rounded-full animate-pulse ${getStatusColor()}`}></span>
                    <span className="text-xs font-mono font-bold text-white uppercase">
            {status || 'OFFLINE'}
          </span>
                </div>
            </div>

            {/* Error Message Display per REQ-USE-030 */}
            {errorMessage && (
                <div className="flex-1 mx-8 bg-red-900/20 border border-red-900 px-4 py-1 rounded text-red-400 text-xs">
                    <strong>Error:</strong> {errorMessage}
                </div>
            )}

            <div className="flex items-center text-gray-500 text-xs italic">
                Last Sync: {new Date().toLocaleTimeString()}
            </div>
        </header>
    );
};

export default Header;