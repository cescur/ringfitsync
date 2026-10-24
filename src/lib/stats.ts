import { formatDuration } from './parser';
import type { Workout } from './workout';

export interface PeriodStats {
  key: string;
  label: string;
  sessions: number;
  totalSec: number;
  totalKcal: number;
  totalM: number;
  avgSec: number;
  avgKcal: number;
  avgM: number;
}

const MONTHS_ES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

function parseDate(date: string): Date {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Lunes de la semana de la fecha dada. */
export function weekStart(date: string): string {
  const d = parseDate(date);
  const offset = (d.getDay() + 6) % 7;
  d.setDate(d.getDate() - offset);
  return iso(d);
}

function weekLabel(start: string): string {
  const s = parseDate(start);
  const e = parseDate(start);
  e.setDate(e.getDate() + 6);
  return `${s.getDate()} ${MONTHS_ES[s.getMonth()]} – ${e.getDate()} ${MONTHS_ES[e.getMonth()]} ${e.getFullYear()}`;
}

function monthLabel(key: string): string {
  const [y, m] = key.split('-').map(Number);
  return `${MONTHS_ES[m - 1]} ${y}`;
}

function summarize(groups: Map<string, Workout[]>, label: (k: string) => string): PeriodStats[] {
  return [...groups.entries()]
    .sort(([a], [b]) => (a < b ? 1 : -1))
    .map(([key, ws]) => {
      const totalSec = ws.reduce((s, w) => s + w.durationSec, 0);
      const totalKcal = ws.reduce((s, w) => s + (w.kcal ?? 0), 0);
      const totalM = ws.reduce((s, w) => s + (w.distanceM ?? 0), 0);
      const n = ws.length;
      return {
        key,
        label: label(key),
        sessions: n,
        totalSec,
        totalKcal,
        totalM,
        avgSec: totalSec / n,
        avgKcal: totalKcal / n,
        avgM: totalM / n,
      };
    });
}

function groupBy(ws: Workout[], keyOf: (w: Workout) => string): Map<string, Workout[]> {
  const map = new Map<string, Workout[]>();
  for (const w of ws) {
    const k = keyOf(w);
    map.set(k, [...(map.get(k) ?? []), w]);
  }
  return map;
}

export function weeklyStats(ws: Workout[]): PeriodStats[] {
  return summarize(groupBy(ws, (w) => weekStart(w.date)), weekLabel);
}

export function monthlyStats(ws: Workout[]): PeriodStats[] {
  return summarize(groupBy(ws, (w) => w.date.slice(0, 7)), monthLabel);
}

export function toCsv(ws: Workout[]): string {
  const header = 'fecha,hora_inicio,duracion,duracion_seg,kcal,distancia_km';
  const lines = [...ws]
    .sort((a, b) => (a.date < b.date ? -1 : 1))
    .map((w) =>
      [
        w.date,
        w.startTime,
        formatDuration(w.durationSec),
        w.durationSec,
        w.kcal ?? '',
        w.distanceM === null ? '' : (w.distanceM / 1000).toFixed(2),
      ].join(','),
    );
  return [header, ...lines].join('\n') + '\n';
}
