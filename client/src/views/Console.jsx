import React, { useState, useEffect, useRef } from 'react';
import { formatESTDate } from '../utils/formatters';

const Console = () => {
    const [logs, setLogs] = useState([]);
    const [command, setCommand] = useState('');
    const scrollRef = useRef(null);

    // Auto-scroll to bottom of logs
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [logs]);

    const handleSendCommand = (e) => {
        e.preventDefault();
        if (!command.trim()) return;

        // Logic to send CMD_ID 0x10 or 0x11 via BinaryPacket goes here
        console.log("Sending command:", command);
        setCommand('');
    };

    const addNewLog = (message, direction) => {
        const newEntry = {
            timestamp: formatESTDate(), // This will be "12-04-2026:22:45:10"
            direction: direction,       // E.g., "SERVER -> CLIENT"
            message: message
        };
        setLogs(prev => [...prev, newEntry]);
    };

    return (
        <div className="flex flex-col h-full space-y-4">
            <h1 className="text-2xl font-bold">Server Console</h1>

            {/* Log Output Window */}
            <div
                ref={scrollRef}
                className="flex-1 bg-black border border-gray-700 rounded p-4 font-mono text-sm overflow-y-auto text-green-400"
            >
                {logs.length === 0 ? (
                    <p className="text-gray-600 italic">Waiting for logs...</p>
                ) : (
                    logs.map((log, index) => (
                        <div key={index} className="mb-1">
                            <span className="text-gray-500">[{log.timestamp}]</span>
                            <span className="text-blue-400 ml-2">[{log.direction}]</span>
                            <span className="ml-2">{log.message}</span>
                        </div>
                    ))
                )}
            </div>

            {/* Command Input Area */}
            <form onSubmit={handleSendCommand} className="flex space-x-2">
                <input
                    type="text"
                    value={command}
                    onChange={(e) => setCommand(e.target.value)}
                    placeholder="Enter server command..."
                    className="flex-1 bg-gray-800 border border-gray-700 rounded px-4 py-2 text-white focus:outline-none focus:border-blue-500"
                />
                <button
                    type="submit"
                    className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded font-bold transition-colors"
                    title="Send command to the Minecraft server"
                >
                    Execute
                </button>
            </form>
        </div>
    );
};

export default Console;