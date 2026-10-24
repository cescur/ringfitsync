import * as SQLite from 'expo-sqlite';

import type { DateOrder } from './parser';
import type { Workout } from './workout';

const db = SQLite.openDatabaseSync('ringfitlog.db');

db.execSync(`
  PRAGMA journal_mode = WAL;
  CREATE TABLE IF NOT EXISTS workouts (
    id TEXT PRIMARY KEY NOT NULL,
    date TEXT NOT NULL,
    start_time TEXT NOT NULL,
    duration_sec INTEGER NOT NULL,
    kcal REAL,
    distance_m REAL,
    in_health_connect INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
  );
  CREATE INDEX IF NOT EXISTS workouts_date ON workouts(date);
  CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
  );
`);

interface Row {
  id: string;
  date: string;
  start_time: string;
  duration_sec: number;
  kcal: number | null;
  distance_m: number | null;
  in_health_connect: number;
}

export interface StoredWorkout extends Workout {
  inHealthConnect: boolean;
}

function fromRow(r: Row): StoredWorkout {
  return {
    id: r.id,
    date: r.date,
    startTime: r.start_time,
    durationSec: r.duration_sec,
    kcal: r.kcal,
    distanceM: r.distance_m,
    inHealthConnect: r.in_health_connect === 1,
  };
}

export async function listWorkouts(): Promise<StoredWorkout[]> {
  const rows = await db.getAllAsync<Row>('SELECT * FROM workouts ORDER BY date DESC, start_time DESC');
  return rows.map(fromRow);
}

export async function workoutsOnDates(dates: string[]): Promise<StoredWorkout[]> {
  if (dates.length === 0) return [];
  const rows = await db.getAllAsync<Row>(
    `SELECT * FROM workouts WHERE date IN (${dates.map(() => '?').join(',')})`,
    dates,
  );
  return rows.map(fromRow);
}

export async function saveWorkout(w: Workout, inHealthConnect: boolean): Promise<void> {
  await db.runAsync(
    `INSERT INTO workouts (id, date, start_time, duration_sec, kcal, distance_m, in_health_connect, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       start_time = excluded.start_time, kcal = excluded.kcal, distance_m = excluded.distance_m,
       in_health_connect = excluded.in_health_connect`,
    [w.id, w.date, w.startTime, w.durationSec, w.kcal, w.distanceM, inHealthConnect ? 1 : 0, new Date().toISOString()],
  );
}

export async function deleteWorkout(id: string): Promise<void> {
  await db.runAsync('DELETE FROM workouts WHERE id = ?', [id]);
}

export interface Settings {
  dateOrder: DateOrder;
  /** HH:MM */
  defaultStartTime: string;
}

const DEFAULTS: Settings = { dateOrder: 'dmy', defaultStartTime: '19:00' };

export async function getSettings(): Promise<Settings> {
  const rows = await db.getAllAsync<{ key: string; value: string }>('SELECT key, value FROM settings');
  const map = Object.fromEntries(rows.map((r) => [r.key, r.value]));
  return {
    dateOrder: map.dateOrder === 'mdy' ? 'mdy' : DEFAULTS.dateOrder,
    defaultStartTime: map.defaultStartTime ?? DEFAULTS.defaultStartTime,
  };
}

export async function setSetting<K extends keyof Settings>(key: K, value: Settings[K]): Promise<void> {
  await db.runAsync(
    'INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value',
    [key, String(value)],
  );
}
