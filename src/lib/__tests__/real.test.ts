import { parseWorkoutLog } from '../parser';

// Texto que devolvió el OCR del móvil para fixtures/registro-es-2026-10-24.png
// (juego en español, captura de la Switch). Los iconos de botones se sustituyeron por texto.
const REGISTRO_ES_2026_10_24 = [
  'Elige una fecha para consultar',
  'el registro de ejercicios del día.',
  'Såbado, 24/10',
  'Tiempo de actividad Calorias quemadas Distancia recorrída',
  '0:00:00 0.00 kcal 0.00 km',
  'Domingo, 20/09',
  'Tiempo de actividad Calorías quemadas Distancia recorrida',
  '0:02:32 21.00 kcal 0.50 km',
  'Sábado, 22/07',
  'Tiempo de actividad Calorías quemadas Distancia recorrida',
  'O:03:19 13.00 kcal 0.50 km',
  'Usa [análogo] y [cruceta] en el JoyCon derecho para desplazarte por las distintas opciones.',
];

describe('capturas reales', () => {
  it('registro en español, 24/10/2026', () => {
    const out = parseWorkoutLog(REGISTRO_ES_2026_10_24, { today: new Date(2026, 9, 24, 20, 0) });
    expect(out).toEqual([
      { date: '2026-10-24', durationSec: 0, kcal: 0, distanceM: 0 },
      { date: '2026-09-20', durationSec: 152, kcal: 21, distanceM: 500 },
      { date: '2026-07-22', durationSec: 199, kcal: 13, distanceM: 500 },
    ]);
  });
});
