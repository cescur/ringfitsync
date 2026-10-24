import {
  formatDuration,
  normalizeOcrText,
  parseDecimalInput,
  parseDurationInput,
  parseWorkoutLog,
  resolveDayMonth,
  resolveYear,
  workoutClientId,
  type OcrLine,
} from '../parser';

const today = new Date(2026, 9, 24); // 24 de octubre de 2026

// Simula líneas del OCR con coordenadas: cada fila en una altura distinta,
// varias columnas en la misma fila.
function rows(...r: string[][]): OcrLine[] {
  return r.flatMap((cols, y) =>
    cols.map((text, x) => ({ text, top: y * 40, bottom: y * 40 + 30, left: x * 300, right: x * 300 + 250 })),
  );
}

describe('pantalla en inglés (como RingFitConnect)', () => {
  it('lee varios días con mes/día, Cal y millas', () => {
    const lines = rows(
      ['Exercise Log'],
      ['10/23 (Fri.)'],
      ['Time Exercising', 'Calories Burned', 'Distance Traveled'],
      ['0:20:49', '74.25 Cal', '0.77 mi.'],
      ['10/22 (Thu.)'],
      ['Time Exercising', 'Calories Burned', 'Distance Traveled'],
      ['0:35:02', '127.10kcal', '1.85km'],
    );
    const out = parseWorkoutLog(lines, { dateOrder: 'mdy', today });
    expect(out).toEqual([
      { date: '2026-10-23', durationSec: 20 * 60 + 49, kcal: 74.25, distanceM: Math.round(0.77 * 1609.344) },
      { date: '2026-10-22', durationSec: 35 * 60 + 2, kcal: 127.1, distanceM: 1850 },
    ]);
  });

  it('funciona con texto sin coordenadas, en orden de lectura', () => {
    const out = parseWorkoutLog(
      ['11/29 (Fri.)', 'Time Exercising 0:20:49', 'Calories Burned 74.25 Cal', 'Distance Traveled 0.77 mi.'],
      { dateOrder: 'mdy', today },
    );
    expect(out).toHaveLength(1);
    // 29 de noviembre aún no ha llegado en 2026: es del año anterior.
    expect(out[0].date).toBe('2025-11-29');
  });
});

describe('pantalla en español', () => {
  it('lee día/mes con coma decimal y kcal', () => {
    const lines = rows(
      ['Registro de ejercicio'],
      ['23/10 (vie.)'],
      ['Tiempo de ejercicio', 'Calorías quemadas', 'Distancia recorrida'],
      ['0:20:49', '74,25 kcal', '1,23 km'],
      ['21/10 (mié.)'],
      ['Tiempo de ejercicio', 'Calorías quemadas', 'Distancia recorrida'],
      ['1:02:10', '210,50 kcal', '3,40 km'],
      ['20/10 (mar.)'],
      ['Tiempo de ejercicio', 'Calorías quemadas', 'Distancia recorrida'],
      ['0:15:00', '50,00 kcal', '0,80 km'],
    );
    const out = parseWorkoutLog(lines, { today });
    expect(out).toEqual([
      { date: '2026-10-23', durationSec: 1249, kcal: 74.25, distanceM: 1230 },
      { date: '2026-10-21', durationSec: 3730, kcal: 210.5, distanceM: 3400 },
      { date: '2026-10-20', durationSec: 900, kcal: 50, distanceM: 800 },
    ]);
  });

  it('deduce el orden si un número es mayor que 12', () => {
    const out = parseWorkoutLog(['10/23', '0:10:00', '30 kcal', '0,5 km'], { dateOrder: 'dmy', today });
    expect(out[0].date).toBe('2026-10-23');
  });

  it('tolera O en vez de 0, espacios sueltos y unidades mal leídas', () => {
    const out = parseWorkoutLog(
      rows(['2O/1O (mar.)'], ['O: 2O :49', '74, 25 kcaI', '1,2O krn']),
      { today },
    );
    expect(out).toEqual([{ date: '2026-10-20', durationSec: 1249, kcal: 74.25, distanceM: 1200 }]);
  });

  it('tolera l o I en lugar de 1', () => {
    const out = parseWorkoutLog(['l9/l0', 'Tiempo l:05:00', '3l2,4 kcal', 'I,5 km'], { today });
    expect(out).toEqual([{ date: '2026-10-19', durationSec: 3900, kcal: 312.4, distanceM: 1500 }]);
  });

  it('deja vacíos los campos que faltan en lugar de descartar el día', () => {
    const lines = rows(
      ['23/10 (vie.)'],
      ['0:20:49', '0,77 km'], // faltan las calorías por un reflejo
      ['22/10 (jue.)'],
      ['74,25 kcal'],
      ['21/10 (mié.)'], // fila cortada abajo, sin valores
    );
    const out = parseWorkoutLog(lines, { today });
    expect(out).toEqual([
      { date: '2026-10-23', durationSec: 1249, distanceM: 770 },
      { date: '2026-10-22', kcal: 74.25 },
      { date: '2026-10-21' },
    ]);
  });

  it('ignora valores de una fila cortada antes de la primera fecha', () => {
    const lines = rows(['0:10:00', '40,00 kcal', '0,30 km'], ['23/10 (vie.)'], ['0:20:49', '74,25 kcal', '0,77 km']);
    const out = parseWorkoutLog(lines, { today });
    expect(out).toEqual([{ date: '2026-10-23', durationSec: 1249, kcal: 74.25, distanceM: 770 }]);
  });

  it('acepta fechas con nombre de mes', () => {
    const out = parseWorkoutLog(['23 oct. (vie.)', '0:20:49 74,25 kcal 0,77 km'], { today });
    expect(out[0]).toMatchObject({ date: '2026-10-23', durationSec: 1249 });
  });

  it('ordena por posición vertical aunque el OCR devuelva bloques desordenados', () => {
    const lines = rows(['23/10'], ['0:20:49', '74,25 kcal', '0,77 km'], ['22/10'], ['0:10:00', '30,00 kcal', '0,40 km']);
    const shuffled = [lines[5], lines[0], lines[7], lines[2], lines[4], lines[1], lines[6], lines[3]];
    const out = parseWorkoutLog(shuffled, { today });
    expect(out.map((w) => [w.date, w.durationSec])).toEqual([
      ['2026-10-23', 1249],
      ['2026-10-22', 600],
    ]);
  });

  it('agrupa en la misma fila columnas con alturas ligeramente distintas', () => {
    const lines: OcrLine[] = [
      { text: '23/10 (vie.)', top: 0, bottom: 30, left: 0 },
      { text: '74,25', top: 52, bottom: 80, left: 300 },
      { text: 'kcal', top: 55, bottom: 82, left: 380 },
      { text: '0:20:49', top: 50, bottom: 80, left: 0 },
      { text: '0,77 km', top: 48, bottom: 78, left: 600 },
    ];
    expect(parseWorkoutLog(lines, { today })).toEqual([
      { date: '2026-10-23', durationSec: 1249, kcal: 74.25, distanceM: 770 },
    ]);
  });
});

describe('pantalla "Resultados de hoy"', () => {
  it('usa la fecha de hoy si no hay fechas', () => {
    const out = parseWorkoutLog(
      ['Resultados de hoy', 'Tiempo de ejercicio activo', '20:49', 'Calorías quemadas', '74,25 kcal', 'Distancia recorrida', '0,77 km'],
      { today },
    );
    expect(out).toEqual([
      { date: '2026-10-24', dateIsToday: true, durationSec: 1249, kcal: 74.25, distanceM: 770 },
    ]);
  });

  it('acepta "20 min 49 s"', () => {
    const out = parseWorkoutLog(['Tiempo 20 min 49 s', '74,25 kcal'], { today });
    expect(out[0].durationSec).toBe(1249);
  });

  it('no devuelve nada si no hay valores', () => {
    expect(parseWorkoutLog(['Hola', 'Menú principal'], { today })).toEqual([]);
  });
});

describe('utilidades', () => {
  it('resolveDayMonth', () => {
    expect(resolveDayMonth(5, 3, 'dmy')).toEqual({ day: 5, month: 3 });
    expect(resolveDayMonth(5, 3, 'mdy')).toEqual({ day: 3, month: 5 });
    expect(resolveDayMonth(13, 3, 'mdy')).toEqual({ day: 13, month: 3 });
    expect(resolveDayMonth(13, 13, 'dmy')).toBeNull();
  });

  it('resolveYear usa el año anterior para fechas futuras y valida el día', () => {
    expect(resolveYear(10, 24, today)).toBe(2026);
    expect(resolveYear(10, 25, today)).toBe(2025);
    expect(resolveYear(2, 30, today)).toBeNull();
  });

  it('normalizeOcrText no toca palabras', () => {
    expect(normalizeOcrText('Calorías quemadas 74,25 Cal')).toBe('Calorías quemadas 74,25 cal');
    expect(normalizeOcrText('Distance Traveled (Dom.)')).toBe('Distance Traveled (Dom.)');
  });

  it('id estable a partir de fecha y duración', () => {
    expect(workoutClientId('2026-10-23', 1249)).toBe('ringfit-2026-10-23-1249');
  });

  it('entradas de la revisión', () => {
    expect(formatDuration(1249)).toBe('0:20:49');
    expect(parseDurationInput('0:20:49')).toBe(1249);
    expect(parseDurationInput('20:49')).toBe(1249);
    expect(parseDurationInput('20')).toBe(1200);
    expect(parseDurationInput('abc')).toBeNull();
    expect(parseDecimalInput('74,25')).toBe(74.25);
    expect(parseDecimalInput('')).toBeUndefined();
    expect(parseDecimalInput('7x')).toBeNull();
  });
});
