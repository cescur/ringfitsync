import { monthlyStats, toCsv, weekStart, weeklyStats } from '../stats';
import { endDate, isValidDate, isValidTime, makeWorkout } from '../workout';

const ws = [
  makeWorkout({ date: '2026-10-19', startTime: '19:00', durationSec: 1200, kcal: 60, distanceM: 1000 }), // lunes
  makeWorkout({ date: '2026-10-25', startTime: '19:00', durationSec: 1800, kcal: 90, distanceM: null }), // domingo
  makeWorkout({ date: '2026-10-26', startTime: '20:30', durationSec: 600, kcal: null, distanceM: 500 }), // lunes siguiente
  makeWorkout({ date: '2026-09-30', startTime: '19:00', durationSec: 900, kcal: 45, distanceM: 700 }),
];

describe('estadísticas', () => {
  it('semanas empiezan en lunes', () => {
    expect(weekStart('2026-10-25')).toBe('2026-10-19');
    expect(weekStart('2026-10-19')).toBe('2026-10-19');
    expect(weekStart('2026-10-26')).toBe('2026-10-26');
  });

  it('totales y medias por semana', () => {
    const weeks = weeklyStats(ws);
    expect(weeks.map((w) => w.key)).toEqual(['2026-10-26', '2026-10-19', '2026-09-28']);
    expect(weeks[1]).toMatchObject({ sessions: 2, totalSec: 3000, totalKcal: 150, totalM: 1000, avgSec: 1500 });
  });

  it('totales por mes', () => {
    const months = monthlyStats(ws);
    expect(months.map((m) => [m.key, m.label, m.sessions])).toEqual([
      ['2026-10', 'oct 2026', 3],
      ['2026-09', 'sep 2026', 1],
    ]);
  });

  it('CSV ordenado por fecha con campos vacíos', () => {
    expect(toCsv(ws).split('\n').slice(0, 3)).toEqual([
      'fecha,hora_inicio,duracion,duracion_seg,kcal,distancia_km',
      '2026-09-30,19:00,0:15:00,900,45,0.70',
      '2026-10-19,19:00,0:20:00,1200,60,1.00',
    ]);
  });
});

describe('workout', () => {
  it('id estable y hora de fin', () => {
    const w = ws[0];
    expect(w.id).toBe('ringfit-2026-10-19-1200');
    expect(endDate(w).getHours()).toBe(19);
    expect(endDate(w).getMinutes()).toBe(20);
  });

  it('validaciones', () => {
    expect(isValidTime('19:00')).toBe(true);
    expect(isValidTime('7:05')).toBe(true);
    expect(isValidTime('24:00')).toBe(false);
    expect(isValidDate('2026-02-29')).toBe(false);
    expect(isValidDate('2028-02-29')).toBe(true);
  });
});
