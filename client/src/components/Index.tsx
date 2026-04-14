import ServerHeader from "./ServerHeader";
import ServerStats from "./ServerStats";
import ChatLogs from "./ChatLogs";
import ConfigEditor from "./ConfigEditor";

const Index = () => {
  return (
    <div className="min-h-screen p-4 md:p-6 max-w-7xl mx-auto">
      <ServerHeader />
      <ServerStats />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4" style={{ height: "calc(100vh - 280px)", minHeight: "400px" }}>
        <ChatLogs />
        <ConfigEditor />
      </div>
    </div>
  );
};

export default Index;
