import React, { useState } from 'react';

const FileEditor = () => {
    const [filename, setFilename] = useState('server.properties');
    const [content, setContent] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    // This would be triggered by your BinaryPacket logic
    const handleFetchFile = () => {
        console.log(`Fetching ${filename}...`);
        // Example: sendPacket(0x20, { filename });
    };

    const handleSaveFile = () => {
        setIsSaving(true);
        console.log(`Saving ${filename}...`);
        // Example: sendPacket(0x21, { filename, content });

        // Simulate a successful save
        setTimeout(() => setIsSaving(false), 1000);
    };

    return (
        <div className="flex flex-col h-full space-y-4">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Configuration Editor</h1>
                <div className="flex space-x-2">
                    <select
                        value={filename}
                        onChange={(e) => setFilename(e.target.value)}
                        className="bg-gray-800 border border-gray-700 rounded px-3 py-1 text-sm text-white"
                    >
                        <option value="server.properties">server.properties</option>
                        <option value="whitelist.json">whitelist.json</option>
                        <option value="ops.json">ops.json</option>
                    </select>
                    <button
                        onClick={handleFetchFile}
                        className="bg-gray-700 hover:bg-gray-600 text-white px-4 py-1 rounded text-sm transition-colors"
                        title="Load the selected file from the server"
                    >
                        Load
                    </button>
                </div>
            </div>

            {/* Main Text Area for Editing */}
            <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                spellCheck="false"
                className="flex-1 bg-gray-900 border border-gray-700 rounded p-4 font-mono text-sm text-gray-300 focus:outline-none focus:border-blue-500 resize-none"
                placeholder="# Loading file content..."
            />

            <div className="flex justify-end">
                <button
                    onClick={handleSaveFile}
                    disabled={isSaving}
                    className={`${
                        isSaving ? 'bg-gray-600' : 'bg-green-600 hover:bg-green-700'
                    } text-white px-8 py-2 rounded font-bold transition-colors`}
                    title="Save changes and overwrite the file on the server"
                >
                    {isSaving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
        </div>
    );
};

export default FileEditor;