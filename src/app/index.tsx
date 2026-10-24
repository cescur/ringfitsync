import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { Stack, router } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { Button, Card, colors } from '../components/ui';
import { getSettings } from '../lib/db';
import { readImage } from '../lib/ocr';
import { parseWorkoutLog } from '../lib/parser';
import { setPendingScan } from '../lib/scanStore';
import { HEALTH_STATUS_TEXT, useHealthStatus } from '../lib/useHealthStatus';

function HeaderButton({ icon, label, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} hitSlop={8} accessibilityLabel={label} style={styles.headerButton}>
      <Ionicons name={icon} size={24} color={colors.text} />
    </Pressable>
  );
}

export default function Home() {
  const { status, grant } = useHealthStatus();
  const [busy, setBusy] = useState(false);

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

  const healthOk = status === 'ok';

  return (
    <View style={styles.container}>
      <Stack.Screen
        options={{
          headerRight: () => (
            <View style={styles.headerRight}>
              <Pressable
                onPress={() => router.push('/settings')}
                hitSlop={8}
                accessibilityLabel={status ? HEALTH_STATUS_TEXT[status] : 'Comprobando Health Connect'}
                style={styles.headerButton}
              >
                <Ionicons
                  name={healthOk ? 'heart' : 'heart-dislike-outline'}
                  size={22}
                  color={status === null ? colors.muted : healthOk ? colors.ok : colors.warn}
                />
              </Pressable>
              <HeaderButton icon="stats-chart-outline" label="Historial" onPress={() => router.push('/history')} />
              <HeaderButton icon="settings-outline" label="Ajustes" onPress={() => router.push('/settings')} />
            </View>
          ),
        }}
      />

      {status && !healthOk && (
        <Card style={styles.warning}>
          <Text style={styles.warningText}>{HEALTH_STATUS_TEXT[status]}</Text>
          {status === 'missing-permissions' && (
            <Button
              title="Dar permisos"
              variant="secondary"
              onPress={() => grant().catch((e) => Alert.alert('Health Connect', String(e)))}
              style={{ marginTop: 10 }}
            />
          )}
        </Card>
      )}

      <View style={styles.hero}>
        <View style={styles.iconCircle}>
          <Ionicons name="fitness" size={56} color={colors.accent} />
        </View>
        <Text style={styles.title}>Importar entrenamientos</Text>
        <Text style={styles.subtitle}>
          Haz una foto a la pantalla «Registro de ejercicio» de Ring Fit Adventure o elige una captura de la Switch.
        </Text>
      </View>

      <View style={styles.actions}>
        {busy ? (
          <View style={styles.busy}>
            <ActivityIndicator size="large" color={colors.accent} />
            <Text style={styles.subtitle}>Leyendo la imagen…</Text>
          </View>
        ) : (
          <>
            <Pressable
              onPress={() => scan('camera')}
              style={({ pressed }) => [styles.bigButton, pressed && { opacity: 0.85 }]}
              accessibilityRole="button"
            >
              <Ionicons name="camera" size={28} color={colors.accentText} />
              <Text style={styles.bigButtonText}>Hacer foto</Text>
            </Pressable>
            <Button title="Elegir de la galería" variant="secondary" onPress={() => scan('library')} />
          </>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, gap: 16 },
  headerRight: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  headerButton: { padding: 6 },
  warning: { borderColor: colors.warn },
  warningText: { fontSize: 15, color: colors.warn, lineHeight: 21 },
  hero: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 },
  iconCircle: {
    width: 112,
    height: 112,
    borderRadius: 56,
    backgroundColor: '#FDE9DE',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  title: { fontSize: 26, fontWeight: '800', color: colors.text, textAlign: 'center' },
  subtitle: { fontSize: 16, color: colors.muted, lineHeight: 22, textAlign: 'center', paddingHorizontal: 8 },
  actions: { gap: 12, paddingBottom: 12 },
  bigButton: {
    minHeight: 64,
    borderRadius: 16,
    backgroundColor: colors.accent,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  bigButtonText: { color: colors.accentText, fontSize: 19, fontWeight: '700' },
  busy: { alignItems: 'center', gap: 10, paddingVertical: 20 },
});
