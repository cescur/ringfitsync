import { workoutClientId } from './parser';

/** Entrenamiento ya confirmado, tal como se guarda en el historial y en Health Connect. */
export interface Workout {
  id: string;
  /** YYYY-MM-DD */
  date: string;
  /** HH:MM, hora local */
  startTime: string;
  durationSec: number;
  kcal: number | null;
  distanceM: number | null;
}

export function makeWorkout(fields: Omit<Workout, 'id'>): Workout {
  return { id: workoutClientId(fields.date, fields.durationSec), ...fields };
}

export function startDate(w: Pick<Workout, 'date' | 'startTime'>): Date {
  const [y, m, d] = w.date.split('-').map(Number);
  const [hh, mm] = w.startTime.split(':').map(Number);
  return new Date(y, m - 1, d, hh, mm, 0, 0);
}

export function endDate(w: Pick<Workout, 'date' | 'startTime' | 'durationSec'>): Date {
  return new Date(startDate(w).getTime() + w.durationSec * 1000);
}

export function isValidTime(t: string): boolean {
  return /^([01]?\d|2[0-3]):[0-5]\d$/.test(t.trim());
}

export function isValidDate(d: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(d)) return false;
  const [y, m, day] = d.split('-').map(Number);
  const dt = new Date(y, m - 1, day);
  return dt.getFullYear() === y && dt.getMonth() === m - 1 && dt.getDate() === day;
}
