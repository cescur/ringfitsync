import type { OcrLine, ParsedWorkout } from './parser';

/** Resultado del último escaneo, que la pantalla de revisión recoge al abrirse. */
export interface Scan {
  imageUri: string;
  lines: OcrLine[];
  workouts: ParsedWorkout[];
}

let pending: Scan | null = null;

export function setPendingScan(scan: Scan): void {
  pending = scan;
}

export function getPendingScan(): Scan | null {
  return pending;
}
