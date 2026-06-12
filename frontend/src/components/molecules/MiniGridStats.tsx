import type { IconComponent } from "~/types/navigation";
import { Stat } from "./Stat";

interface StatItem {
    label: string;
    value: string;
    hint?: string;
    icon: IconComponent;
}

interface MiniGridStatsProps {
    items: StatItem[];
}

export function MiniGridStats({ items }: MiniGridStatsProps) {
    return (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {items.map((item) => (
                <Stat key={item.label} {...item} />
            ))}
        </div>
    );
}
