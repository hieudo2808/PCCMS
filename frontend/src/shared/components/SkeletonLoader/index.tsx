interface SkeletonLoaderProps {
    rows?: number;
    className?: string;
}

export function SkeletonLoader({ rows = 3, className = "" }: SkeletonLoaderProps) {
    return (
        <div className={`space-y-3 ${className}`}>
            {Array.from({ length: rows }).map((_, i) => (
                <div key={i} className="h-4 bg-gray-200 rounded animate-pulse" />
            ))}
        </div>
    );
}
