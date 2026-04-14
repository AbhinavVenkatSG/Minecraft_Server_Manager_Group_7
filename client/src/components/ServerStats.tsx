import { Cpu, HardDrive, MemoryStick } from "lucide-react";

interface StatGaugeProps {
  label: string;
  value: number;
  max: string;
  icon: React.ReactNode;
  unit: string;
}

const StatGauge = ({ label, value, max, icon, unit }: StatGaugeProps) => {
  const percentage = Math.min(value, 100);
  const color =
    percentage > 85 ? "bg-destructive" : percentage > 60 ? "bg-warning" : "bg-primary";

  return (
    <div className="rounded-lg border border-border bg-card p-4 glow-green">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-primary">{icon}</span>
        <span className="text-sm font-medium text-foreground">{label}</span>
      </div>
      <div className="text-2xl font-bold font-mono text-foreground mb-1">
        {percentage}
        <span className="text-sm text-muted-foreground">{unit}</span>
      </div>
      <div className="text-xs text-muted-foreground mb-2">{max}</div>
      <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-700 ${color}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
};

export default function ServerStats() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <StatGauge
        label="CPU Usage"
        value={42}
        max="AMD Ryzen 9 5900X"
        icon={<Cpu size={18} />}
        unit="%"
      />
      <StatGauge
        label="Memory"
        value={68}
        max="12 GB / 16 GB allocated"
        icon={<MemoryStick size={18} />}
        unit="%"
      />
      <StatGauge
        label="Storage"
        value={34}
        max="17 GB / 50 GB SSD"
        icon={<HardDrive size={18} />}
        unit="%"
      />
    </div>
  );
}
