import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';

import { getHealthStatus, requestHealthPermissions, type HealthStatus } from './health';

export const HEALTH_STATUS_TEXT: Record<HealthStatus, string> = {
  ok: 'Conectado a Health Connect.',
  'missing-permissions': 'Falta dar permiso para guardar en Health Connect.',
  unavailable: 'Health Connect no está disponible en este móvil.',
  'needs-update': 'Hay que actualizar Health Connect desde Google Play.',
};

/** Estado de Health Connect, refrescado cada vez que la pantalla gana el foco. */
export function useHealthStatus() {
  const [status, setStatus] = useState<HealthStatus | null>(null);

  useFocusEffect(
    useCallback(() => {
      getHealthStatus()
        .then(setStatus)
        .catch(() => setStatus('unavailable'));
    }, []),
  );

  const grant = useCallback(async () => {
    setStatus(await requestHealthPermissions());
  }, []);

  return { status, grant };
}
