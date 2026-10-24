import * as ImagePicker from 'expo-image-picker';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Button, Card, colors } from '../components/ui';
import { getSettings } from '../lib/db';
import {
  getHealthStatus,
  openHealthConnectSettings,
  requestHealthPermissions,
  type HealthStatus,
} from '../lib/health';
import { readImage } from '../lib/ocr';
import { parseWorkoutLog } from '../lib/parser';
import { setPendingScan } from '../lib/scanStore';

const STATUS_TEXT: Record<HealthStatus, string> = {
  ok: 'Conectado a Health Connect.',
  'missing-permissions': 'Falta dar permiso para guardar en Health Connect.',
  unavailable: 'Health Connect no está disponible en este móvil.',
  'needs-update': 'Hay que actualizar Health Connect desde Google Play.',
};

export default function Home() {
  const [status, setStatus] = useState<HealthStatus | null>(null);
  const [busy, setBusy] = useState(false);

  useFocusEffect(
    useCallback(() => {
      getHealthStatus()
        .then(setStatus)
        .catch(() => setStatus('unavailable'));
    }, []),
  );

  const grant = async () => {
    try {
      setStatus(await requestHealthPermissions());
    } catch (e) {
      Alert.alert('Health Connect', String(e));
    }
  };

  const scan = async (source: 'camera' | 'library') => {
    try {
      let result: ImagePicker.ImagePickerResult;
      if (source === 'camera') {
        const perm = await ImagePicker.requestCameraPermissionsAsync();
        if (!perm.granted) {
          Alert.alert('Cámara', 'Hace falta permiso de cámara para hacer la foto.');
          return;
        }
        result = await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 1 });
      } else {
        result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 1 });
      }
      if (result.canceled || !result.assets?.[0]) return;

      setBusy(true);
      const imageUri = result.assets[0].uri;
      const lines = await readImage(imageUri);
      const settings = await getSettings();
      const workouts = parseWorkoutLog(lines, { dateOrder: settings.dateOrder });
      setPendingScan({ imageUri, lines, workouts });
      router.push('/review');
    } catch (e) {
      Alert.alert('No se pudo leer la imagen', String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Card>
        <Text style={styles.title}>Importar entrenamientos</Text>
        <Text style={styles.muted}>
          Haz una foto a la pantalla «Registro de ejercicio» de Ring Fit Adventure o elige una captura de la
          Switch desde la galería.
        </Text>
        <View style={styles.gap}>
          <Button title="Hacer foto" onPress={() => scan('camera')} loading={busy} />
          <Button title="Elegir de la galería" variant="secondary" onPress={() => scan('library')} disabled={busy} />
        </View>
      </Card>

      <Card>
        <Text style={styles.title}>Health Connect</Text>
        <Text style={[styles.muted, status === 'ok' && { color: colors.ok }]}>
          {status ? STATUS_TEXT[status] : 'Comprobando…'}
        </Text>
        {status === 'missing-permissions' && (
          <Button title="Dar permisos" onPress={grant} style={{ marginTop: 12 }} />
        )}
        {status && status !== 'unavailable' && (
          <Button
            title="Abrir Health Connect"
            variant="secondary"
            onPress={openHealthConnectSettings}
            style={{ marginTop: 12 }}
          />
        )}
      </Card>

      <View style={styles.gap}>
        <Button title="Historial y estadísticas" variant="secondary" onPress={() => router.push('/history')} />
        <Button title="Ajustes" variant="secondary" onPress={() => router.push('/settings')} />
      </View>

      <Text style={styles.privacy}>
        Privacidad: el texto se reconoce en el propio móvil y los datos solo se guardan aquí y en Health Connect.
        La app no se conecta a internet ni comparte nada salvo cuando exportas el CSV.
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 16 },
  title: { fontSize: 18, fontWeight: '700', color: colors.text, marginBottom: 6 },
  muted: { fontSize: 15, color: colors.muted, lineHeight: 21 },
  gap: { gap: 10, marginTop: 12 },
  privacy: { fontSize: 13, color: colors.muted, lineHeight: 18, marginTop: 4 },
});
