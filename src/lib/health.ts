import {
  ExerciseType,
  RecordingMethod,
  SdkAvailabilityStatus,
  deleteRecordsByUuids,
  getGrantedPermissions,
  getSdkStatus,
  initialize,
  insertRecords,
  openHealthConnectSettings,
  requestPermission,
  type HealthConnectRecord,
  type Permission,
} from 'react-native-health-connect';

import { endDate, startDate, type Workout } from './workout';

export const PERMISSIONS: Permission[] = [
  { accessType: 'write', recordType: 'ExerciseSession' },
  { accessType: 'write', recordType: 'ActiveCaloriesBurned' },
  { accessType: 'write', recordType: 'Distance' },
  { accessType: 'read', recordType: 'ExerciseSession' },
  { accessType: 'read', recordType: 'ActiveCaloriesBurned' },
  { accessType: 'read', recordType: 'Distance' },
];

export type HealthStatus = 'ok' | 'missing-permissions' | 'unavailable' | 'needs-update';

let initialized = false;

async function ensureInitialized(): Promise<boolean> {
  if (!initialized) initialized = await initialize();
  return initialized;
}

export async function getHealthStatus(): Promise<HealthStatus> {
  const sdk = await getSdkStatus();
  if (sdk === SdkAvailabilityStatus.SDK_UNAVAILABLE) return 'unavailable';
  if (sdk === SdkAvailabilityStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) return 'needs-update';
  if (!(await ensureInitialized())) return 'unavailable';
  const granted = await getGrantedPermissions();
  const has = (p: Permission) =>
    granted.some((g) => g.accessType === p.accessType && g.recordType === p.recordType);
  return PERMISSIONS.filter((p) => p.accessType === 'write').every(has) ? 'ok' : 'missing-permissions';
}

export async function requestHealthPermissions(): Promise<HealthStatus> {
  if (!(await ensureInitialized())) return 'unavailable';
  await requestPermission(PERMISSIONS);
  return getHealthStatus();
}

export { openHealthConnectSettings };

function recordIds(w: Workout) {
  return { session: `${w.id}-session`, kcal: `${w.id}-kcal`, distance: `${w.id}-distance` };
}

/**
 * Escribe la sesión, las calorías y la distancia. Health Connect sobrescribe los registros
 * con el mismo clientRecordId, así que importar dos veces el mismo día no duplica nada.
 */
export async function writeWorkout(w: Workout): Promise<void> {
  if (!(await ensureInitialized())) throw new Error('Health Connect no está disponible');
  const startTime = startDate(w).toISOString();
  const endTime = endDate(w).toISOString();
  const ids = recordIds(w);
  const meta = (clientRecordId: string) => ({
    clientRecordId,
    clientRecordVersion: Date.now(),
    recordingMethod: RecordingMethod.RECORDING_METHOD_MANUAL_ENTRY,
  });

  const records: HealthConnectRecord[] = [
    {
      recordType: 'ExerciseSession',
      exerciseType: ExerciseType.OTHER_WORKOUT,
      title: 'Ring Fit Adventure',
      startTime,
      endTime,
      metadata: meta(ids.session),
    },
  ];
  if (w.kcal !== null && w.kcal > 0) {
    records.push({
      recordType: 'ActiveCaloriesBurned',
      energy: { value: w.kcal, unit: 'kilocalories' },
      startTime,
      endTime,
      metadata: meta(ids.kcal),
    });
  }
  if (w.distanceM !== null && w.distanceM > 0) {
    records.push({
      recordType: 'Distance',
      distance: { value: w.distanceM, unit: 'meters' },
      startTime,
      endTime,
      metadata: meta(ids.distance),
    });
  }
  await insertRecords(records);
}

export async function deleteWorkoutFromHealth(w: Workout): Promise<void> {
  if (!(await ensureInitialized())) throw new Error('Health Connect no está disponible');
  const ids = recordIds(w);
  await deleteRecordsByUuids('ExerciseSession', [], [ids.session]);
  await deleteRecordsByUuids('ActiveCaloriesBurned', [], [ids.kcal]);
  await deleteRecordsByUuids('Distance', [], [ids.distance]);
}
