// Lector puro de la pantalla "Registro de ejercicio" de Ring Fit Adventure.
// Sin dependencias nativas: recibe las líneas del OCR y devuelve los días detectados.
// Basado en la lógica de WorkoutLogManager.swift de RingFitConnect (MIT).

export type DateOrder = 'dmy' | 'mdy';

export interface OcrLine {
  text: string;
  /** Coordenadas en píxeles de la imagen; opcionales para tests con texto plano. */
  top?: number;
  bottom?: number;
  left?: number;
  right?: number;
}

export interface ParsedWorkout {
  /** YYYY-MM-DD */
  date: string;
  durationSec?: number;
  kcal?: number;
  distanceM?: number;
  /** true si la pantalla no tenía fecha (p. ej. "Resultados de hoy") y se usó la de hoy. */
  dateIsToday?: boolean;
}

export interface ParseOptions {
  dateOrder?: DateOrder;
  /** Fecha de referencia para deducir el año. Por defecto, ahora. */
  today?: Date;
}

const METERS_PER_MILE = 1609.344;

const NUM = String.raw`(\d+(?:[.,]\d+)?)`;

// Fecha: 29/11, 11/29, 29-11 o 29.11, seguida o no del día de la semana. Se exige que
// no sea parte de un número más largo ni de una hora.
const DATE_RE = /(?<![\d:.,])(\d{1,2})\s?[/\-]\s?(\d{1,2})(?![\d:]|[.,]\d)/g;
// Fecha con nombre de mes: "29 nov." o "nov. 29".
const MONTHS: Record<string, number> = {
  ene: 1, jan: 1, feb: 2, mar: 3, abr: 4, apr: 4, may: 5, jun: 6, jul: 7,
  ago: 8, aug: 8, sep: 9, set: 9, oct: 10, nov: 11, dic: 12, dec: 12,
};
const MONTH_NAMES = Object.keys(MONTHS).join('|');
const DATE_DAY_MONTH_RE = new RegExp(String.raw`(?<![\d:.,])(\d{1,2})\s?(?:de\s)?(${MONTH_NAMES})[a-z]*\.?(?![a-z])`, 'gi');
const DATE_MONTH_DAY_RE = new RegExp(String.raw`(?<![a-z])(${MONTH_NAMES})[a-z]*\.?\s?(\d{1,2})(?![\d:.,])`, 'gi');

// Duración: 0:20:49 (h:mm:ss). También 20:49 (mm:ss) y "20 min 49 s".
const DURATION_HMS_RE = /(?<![\d.,])(\d{1,2}):([0-5]\d):([0-5]\d)(?![\d])/g;
const DURATION_MS_RE = /(?<![\d:.,])([0-5]?\d):([0-5]\d)(?![\d:])/g;
const DURATION_WORDS_RE = /(?:(\d{1,2})\s?h\s?)?(\d{1,3})\s?min(?:\.|utos?)?(?:\s?(\d{1,2})\s?(?:s|seg|segundos?)\b)?/gi;

const KCAL_RE = new RegExp(String.raw`${NUM}\s?(kcal|cal)(?![a-z])`, 'gi');
const DIST_RE = new RegExp(String.raw`${NUM}\s?(km|mi\.?|millas?)(?![a-z])`, 'gi');

/**
 * Corrige errores típicos del OCR antes de buscar valores:
 * O/o por 0, I/l/| por 1 junto a dígitos, espacios dentro de números y unidades mal leídas.
 */
export function normalizeOcrText(input: string): string {
  let s = input;
  // Unidades mal leídas: "kcaI", "Ca1", "krn".
  s = s.replace(/\b(k?)ca[lI1|]\b/gi, (_, k: string) => `${k}cal`);
  s = s.replace(/(\d|\s)krn\b/gi, '$1km');
  // Letras parecidas a dígitos cuando están pegadas a un número o separador numérico.
  for (let i = 0; i < 2; i++) {
    s = s.replace(/(?<=\d[:.,/]?)[OoQD]|(?<![A-Za-zÀ-ÿ])[OoQD](?=[:.,/]?\d)/g, '0');
    s = s.replace(/(?<=\d[:.,/]?)[Il|](?![A-Za-zÀ-ÿ])|(?<![A-Za-zÀ-ÿ])[Il|](?=[:.,/]?\d)/g, '1');
    s = s.replace(/(?<=\d)[Ss](?=\d)/g, '5');
  }
  // Espacios sueltos dentro de horas y decimales: "0 :20: 49", "74, 25".
  s = s.replace(/(\d)\s*([:])\s*(\d)/g, '$1$2$3');
  s = s.replace(/(\d)\s*([:])\s*(\d)/g, '$1$2$3');
  s = s.replace(/(\d)([.,])\s+(\d{2})(?!\d)/g, '$1$2$3');
  s = s.replace(/[ \t]+/g, ' ');
  return s;
}

function toNumber(raw: string): number {
  return parseFloat(raw.replace(',', '.'));
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

export function formatDate(year: number, month: number, day: number): string {
  return `${year}-${pad(month)}-${pad(day)}`;
}

/** Decide qué número es el día y cuál el mes. Devuelve null si no es una fecha válida. */
export function resolveDayMonth(a: number, b: number, order: DateOrder): { day: number; month: number } | null {
  let day: number;
  let month: number;
  if (a > 12 && b <= 12) {
    day = a; month = b;
  } else if (b > 12 && a <= 12) {
    day = b; month = a;
  } else if (order === 'dmy') {
    day = a; month = b;
  } else {
    day = b; month = a;
  }
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  return { day, month };
}

/** El año no sale en pantalla: el actual, o el anterior si la fecha quedaría en el futuro. */
export function resolveYear(month: number, day: number, today: Date): number | null {
  let year = today.getFullYear();
  const todayKey = (today.getMonth() + 1) * 100 + today.getDate();
  if (month * 100 + day > todayKey) year -= 1;
  if (day > daysInMonth(year, month)) return null;
  return year;
}

type Token =
  | { kind: 'date'; index: number; month: number; day: number }
  | { kind: 'duration'; index: number; seconds: number }
  | { kind: 'kcal'; index: number; value: number }
  | { kind: 'distance'; index: number; meters: number };

function overlaps(taken: Array<[number, number]>, start: number, end: number): boolean {
  return taken.some(([s, e]) => start < e && end > s);
}

/** Busca fechas, duraciones, calorías y distancias en una fila de texto, en orden. */
export function tokenize(text: string, order: DateOrder): Token[] {
  const tokens: Token[] = [];
  const taken: Array<[number, number]> = [];
  const add = (t: Token, start: number, end: number) => {
    if (overlaps(taken, start, end)) return;
    taken.push([start, end]);
    tokens.push(t);
  };

  for (const m of text.matchAll(DURATION_HMS_RE)) {
    const seconds = +m[1] * 3600 + +m[2] * 60 + +m[3];
    add({ kind: 'duration', index: m.index!, seconds }, m.index!, m.index! + m[0].length);
  }
  for (const m of text.matchAll(KCAL_RE)) {
    add({ kind: 'kcal', index: m.index!, value: toNumber(m[1]) }, m.index!, m.index! + m[0].length);
  }
  for (const m of text.matchAll(DIST_RE)) {
    const value = toNumber(m[1]);
    const unit = m[2].toLowerCase();
    const meters = unit.startsWith('km') ? value * 1000 : value * METERS_PER_MILE;
    add({ kind: 'distance', index: m.index!, meters }, m.index!, m.index! + m[0].length);
  }
  for (const m of text.matchAll(DURATION_WORDS_RE)) {
    const seconds = (+(m[1] ?? 0)) * 3600 + +m[2] * 60 + +(m[3] ?? 0);
    add({ kind: 'duration', index: m.index!, seconds }, m.index!, m.index! + m[0].length);
  }
  for (const m of text.matchAll(DATE_RE)) {
    const dm = resolveDayMonth(+m[1], +m[2], order);
    if (dm) add({ kind: 'date', index: m.index!, ...dm }, m.index!, m.index! + m[0].length);
  }
  for (const m of text.matchAll(DATE_DAY_MONTH_RE)) {
    const month = MONTHS[m[2].slice(0, 3).toLowerCase()];
    const day = +m[1];
    if (day >= 1 && day <= 31) add({ kind: 'date', index: m.index!, day, month }, m.index!, m.index! + m[0].length);
  }
  for (const m of text.matchAll(DATE_MONTH_DAY_RE)) {
    const month = MONTHS[m[1].slice(0, 3).toLowerCase()];
    const day = +m[2];
    if (day >= 1 && day <= 31) add({ kind: 'date', index: m.index!, day, month }, m.index!, m.index! + m[0].length);
  }
  // mm:ss solo si no hay otra cosa en esa posición (pantalla "Resultados de hoy").
  for (const m of text.matchAll(DURATION_MS_RE)) {
    const seconds = +m[1] * 60 + +m[2];
    add({ kind: 'duration', index: m.index!, seconds }, m.index!, m.index! + m[0].length);
  }

  return tokens.sort((a, b) => a.index - b.index);
}

/**
 * Agrupa las líneas del OCR en filas por posición vertical y las ordena de
 * arriba abajo y de izquierda a derecha. Sin coordenadas, respeta el orden recibido.
 */
export function groupIntoRows(lines: OcrLine[]): string[] {
  const withPos = lines.filter((l) => l.text.trim() !== '');
  if (!withPos.every((l) => l.top !== undefined && l.bottom !== undefined)) {
    return withPos.map((l) => l.text);
  }
  const items = withPos
    .map((l) => ({
      text: l.text,
      center: (l.top! + l.bottom!) / 2,
      height: Math.max(1, l.bottom! - l.top!),
      left: l.left ?? 0,
    }))
    .sort((a, b) => a.center - b.center);

  const rows: Array<typeof items> = [];
  for (const item of items) {
    const row = rows[rows.length - 1];
    if (row) {
      const rowCenter = row.reduce((s, i) => s + i.center, 0) / row.length;
      const rowHeight = row.reduce((s, i) => s + i.height, 0) / row.length;
      if (Math.abs(item.center - rowCenter) < Math.min(rowHeight, item.height) * 0.5) {
        row.push(item);
        continue;
      }
    }
    rows.push([item]);
  }
  return rows.map((row) => row.sort((a, b) => a.left - b.left).map((i) => i.text).join(' '));
}

/**
 * Extrae todos los días de la pantalla. Recorre los valores en orden de lectura:
 * cada fecha abre un entrenamiento nuevo y los valores siguientes se le asignan.
 */
export function parseWorkoutLog(lines: OcrLine[] | string[], options: ParseOptions = {}): ParsedWorkout[] {
  const order = options.dateOrder ?? 'dmy';
  const today = options.today ?? new Date();
  const ocrLines: OcrLine[] = (lines as Array<OcrLine | string>).map((l) => (typeof l === 'string' ? { text: l } : l));

  const tokens = groupIntoRows(ocrLines).flatMap((row) => tokenize(normalizeOcrText(row), order));
  const hasDates = tokens.some((t) => t.kind === 'date');

  const result: ParsedWorkout[] = [];
  let current: ParsedWorkout | null = null;

  if (!hasDates) {
    // Pantalla sin fechas ("Resultados de hoy"): un único entrenamiento con la fecha de hoy.
    current = {
      date: formatDate(today.getFullYear(), today.getMonth() + 1, today.getDate()),
      dateIsToday: true,
    };
  }

  for (const t of tokens) {
    if (t.kind === 'date') {
      const year = resolveYear(t.month, t.day, today);
      if (year === null) continue;
      // Un día sin valores también se guarda: la revisión lo deja vacío y editable.
      if (current) result.push(current);
      current = { date: formatDate(year, t.month, t.day) };
      continue;
    }
    // Valores antes de la primera fecha (fila cortada arriba): se ignoran.
    if (!current) continue;
    if (t.kind === 'duration' && current.durationSec === undefined) current.durationSec = t.seconds;
    else if (t.kind === 'kcal' && current.kcal === undefined) current.kcal = t.value;
    else if (t.kind === 'distance' && current.distanceM === undefined) current.distanceM = Math.round(t.meters);
  }
  if (current && (hasDates || hasValues(current))) result.push(current);

  // Si la misma fecha aparece dos veces (foto repetida o fila partida), se combinan.
  const byDate = new Map<string, ParsedWorkout>();
  for (const w of result) {
    const prev = byDate.get(w.date);
    if (!prev) byDate.set(w.date, w);
    else {
      prev.durationSec ??= w.durationSec;
      prev.kcal ??= w.kcal;
      prev.distanceM ??= w.distanceM;
    }
  }
  return [...byDate.values()].sort((a, b) => (a.date < b.date ? 1 : -1));
}

export function hasValues(w: ParsedWorkout): boolean {
  return w.durationSec !== undefined || w.kcal !== undefined || w.distanceM !== undefined;
}

/** Identificador estable para evitar duplicados en Health Connect y en el historial. */
export function workoutClientId(date: string, durationSec: number): string {
  return `ringfit-${date}-${durationSec}`;
}

export function formatDuration(totalSec: number): string {
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = Math.round(totalSec % 60);
  return `${h}:${pad(m)}:${pad(s)}`;
}

/** Acepta "0:20:49", "20:49" o "20". Devuelve segundos o null. */
export function parseDurationInput(input: string): number | null {
  const parts = input.trim().split(':').map((p) => p.trim());
  if (parts.some((p) => !/^\d+$/.test(p))) return null;
  const nums = parts.map(Number);
  if (nums.length === 3) return nums[0] * 3600 + nums[1] * 60 + nums[2];
  if (nums.length === 2) return nums[0] * 60 + nums[1];
  if (nums.length === 1) return nums[0] * 60;
  return null;
}

/** Acepta "74,25" o "74.25". Vacío devuelve undefined; texto inválido, null. */
export function parseDecimalInput(input: string): number | undefined | null {
  const t = input.trim();
  if (t === '') return undefined;
  if (!/^\d+([.,]\d+)?$/.test(t)) return null;
  return toNumber(t);
}
