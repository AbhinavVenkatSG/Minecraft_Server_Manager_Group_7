import React from 'react';

const Sidebar = ({ activeTab, setActiveTab }) => {
    // Grouped menu items as per REQ-USE-010 and REQ-USE-020
    const menuGroups = [
        {
            title: "Monitoring",
            items: [
                { id: 'dashboard', label: 'Telemetry', desc: 'Real-time CPU, RAM, and Storage usage' },
                { id: 'console', label: 'Server Console', desc: 'View live chat logs and player count' },
            ]
        },
        {
            title: "Management",
            items: [
                { id: 'files', label: 'File Editor', desc: 'Edit server.properties and whitelist' },
                { id: 'backups', label: 'Backups', desc: 'Create and export world backups' },
            ]
        }
    ];

    return (
        <aside className="w-64 bg-gray-900 border-r border-gray-800 flex flex-col">
            <div className="p-6 font-bold text-xl tracking-widest text-blue-400">
                MC MANAGER
            </div>

            <nav className="flex-1 px-4">
                {menuGroups.map((group) => (
                    <div key={group.title} className="mb-8">
                        {/* Group Header to avoid clutter */}
                        <h2 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-4 px-2">
                            {group.title}
                        </h2>

                        {group.items.map((item) => (
                            <button
                                key={item.id}
                                onClick={() => setActiveTab(item.id)}
                                // Hover description logic
                                title={item.desc}
                                className={`w-full flex items-center px-2 py-2 mb-1 text-sm font-medium rounded-md transition-colors ${
                                    activeTab === item.id
                                        ? 'bg-gray-800 text-white'
                                        : 'text-gray-400 hover:text-white hover:bg-gray-800'
                                }`}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>
                ))}
            </nav>

            <div className="p-4 border-t border-gray-800">
                <button className="text-xs text-gray-500 hover:text-red-400 transition-colors">
                    Logout
                </button>
            </div>
        </aside>
    );
};

export default Sidebar;