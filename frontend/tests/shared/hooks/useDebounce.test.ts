import { renderHook, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { useDebounce } from "~/shared/hooks/useDebounce";

describe("useDebounce", () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it("should return the initial value immediately", () => {
        const { result } = renderHook(() => useDebounce("initial", 500));
        expect(result.current).toBe("initial");
    });

    it("should debounce the value change", () => {
        const { result, rerender } = renderHook(({ value }) => useDebounce(value, 500), {
            initialProps: { value: "initial" },
        });

        // Update value
        rerender({ value: "updated" });

        // Value should not update immediately
        expect(result.current).toBe("initial");

        // Fast-forward time
        act(() => {
            vi.advanceTimersByTime(500);
        });

        // Now value should be updated
        expect(result.current).toBe("updated");
    });
});
