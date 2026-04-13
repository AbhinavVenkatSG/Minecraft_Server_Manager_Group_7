export const formatESTDate = (date = new Date()) => {
    const options = {
        timeZone: 'America/New_York', // Forces EST/EDT
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false, // Forces 24-hour format (hh:mm:ss)
    };

    const formatter = new Intl.DateTimeFormat('en-CA', options);
    const parts = formatter.formatToParts(date);

    // Map parts to the required dd-mm-yyyy:hh-mm-ss format
    const d = parts.find(p => p.type === 'day').value;
    const m = parts.find(p => p.type === 'month').value;
    const y = parts.find(p => p.type === 'year').value;
    const hh = parts.find(p => p.type === 'hour').value;
    const mm = parts.find(p => p.type === 'minute').value;
    const ss = parts.find(p => p.type === 'second').value;

    return `${d}-${m}-${y}:${hh}-${mm}-${ss}`;
};