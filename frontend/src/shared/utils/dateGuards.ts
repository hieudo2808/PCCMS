const CLINIC_TIME_ZONE = "Asia/Ho_Chi_Minh";

export function clinicTodayIso(): string {
    return new Intl.DateTimeFormat("en-CA", { timeZone: CLINIC_TIME_ZONE }).format(new Date());
}

export function addDaysIso(dateIso: string, days: number): string {
    const [year, month, day] = dateIso.split("-").map(Number);
    const date = new Date(year, month - 1, day);
    date.setDate(date.getDate() + days);
    return toIsoDate(date);
}

export function toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

export function isDateRangeValid(fromDate: string, toDate: string, maxDays?: number): boolean {
    if (!fromDate || !toDate || toDate < fromDate) return false;
    if (maxDays == null) return true;
    const from = new Date(`${fromDate}T00:00:00`).getTime();
    const to = new Date(`${toDate}T00:00:00`).getTime();
    return (to - from) / (24 * 60 * 60 * 1000) <= maxDays;
}

export function isFutureLocalDateTime(date: string, time: string): boolean {
    if (!date || !time) return false;
    return new Date(`${date}T${time}`).getTime() > Date.now();
}
