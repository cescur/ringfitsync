import Constants from 'expo-constants';
import { useEffect, useState } from 'react';
import { Alert, Linking, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { PickerField } from '../components/PickerField';
import { Button, Card, colors } from '../components/ui';
import { getSettings, setSetting, type Settings } from '../lib/db';
import { openHealthConnectSettings } from '../lib/health';
import type { DateOrder } from '../lib/parser';
import { HEALTH_STATUS_TEXT, useHealthStatus } from '../lib/useHealthStatus';

const REPO_URL = 'https://github.com/cescur/ringfitsync';
const INSPIRATION_URL = 'https://github.com/watanabetoshinori/RingFitConnect';

export default function SettingsScreen() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const { status, grant } = useHealthStatus();

  useEffect(() => {
    getSettings().then((s) => {
      setSettings(s);
    });
  }, []);

  if (!settings) return null;

  const setOrder = async (dateOrder: DateOrder) => {
    await setSetting('dateOrder', dateOrder);
    setSettings({ ...settings, dateOrder });
  };

  const saveStart = async (value: string) => {
    await setSetting('defaultStartTime', value);
    setSettings({ ...settings, defaultStartTime: value });
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Card>
        <Text style={styles.title}>Formato de fecha en el juego</Text>
        <Text style={styles.muted}>Si un número es mayor que 12, el orden se deduce solo.</Text>
        {(
          [
            ['dmy', 'Día/mes (29/11)'],
            ['mdy', 'Mes/día (11/29)'],
          ] as const
        ).map(([value, label]) => (
          <Pressable key={value} onPress={() => setOrder(value)} style={styles.option}>
            <View style={[styles.radio, settings.dateOrder === value && styles.radioOn]} />
            <Text style={styles.optionText}>{label}</Text>
          </Pressable>
        ))}
      </Card>

      <Card>
        <Text style={styles.title}>Hora de inicio por defecto</Text>
        <Text style={styles.muted}>
          La pantalla del juego no dice a qué hora entrenaste. Se usa esta hora y puedes cambiarla en la revisión.
        </Text>
        <PickerField
          mode="time"
          value={settings.defaultStartTime}
          onChange={saveStart}
          title="Hora de inicio por defecto"
          style={{ marginTop: 10, width: 120 }}
        />
      </Card>

      <Card>
        <Text style={styles.title}>Health Connect</Text>
        <Text style={[styles.muted, status === 'ok' && { color: colors.ok }]}>
          {status ? HEALTH_STATUS_TEXT[status] : 'Comprobando…'}
        </Text>
        {status === 'missing-permissions' && (
          <Button
            title="Dar permisos"
            onPress={() => grant().catch((e) => Alert.alert('Health Connect', String(e)))}
            style={{ marginTop: 12 }}
          />
        )}
        {status && status !== 'unavailable' && (
          <Pressable onPress={openHealthConnectSettings} style={{ marginTop: 12 }}>
            <Text style={styles.link}>Abrir ajustes de Health Connect</Text>
          </Pressable>
        )}
      </Card>

      <Card>
        <Text style={styles.title}>Privacidad</Text>
        <Text style={styles.muted}>
          El texto de las fotos se reconoce en el propio móvil y los entrenamientos solo se guardan en esta app y en
          Health Connect. La app no usa internet, no tiene anuncios ni analítica, y solo comparte datos cuando tú
          exportas el CSV.
        </Text>
      </Card>

      <Card>
        <Text style={styles.title}>Acerca de</Text>
        <Text style={styles.muted}>Versión {Constants.expoConfig?.version ?? '–'}</Text>
        <Text style={[styles.muted, { marginTop: 8 }]}>
          Esta app no está afiliada, patrocinada ni aprobada por Nintendo. Nintendo Switch y Ring Fit Adventure son
          marcas registradas de Nintendo. Health Connect es una marca de Google LLC.
        </Text>
        <Text style={[styles.muted, { marginTop: 8 }]}>
          Software libre con licencia MIT. La lógica de lectura se inspira en{' '}
          <Text style={styles.inlineLink} onPress={() => Linking.openURL(INSPIRATION_URL)}>
            RingFitConnect
          </Text>{' '}
          de Toshinori Watanabe (MIT).
        </Text>
        <Pressable onPress={() => Linking.openURL(REPO_URL)} style={{ marginTop: 12 }}>
          <Text style={styles.link}>Código fuente en GitHub</Text>
        </Pressable>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 14 },
  title: { fontSize: 16, fontWeight: '700', color: colors.text, marginBottom: 4 },
  muted: { fontSize: 14, color: colors.muted, lineHeight: 20 },
  option: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 10 },
  radio: { width: 20, height: 20, borderRadius: 10, borderWidth: 2, borderColor: colors.border },
  radioOn: { borderColor: colors.accent, backgroundColor: colors.accent },
  optionText: { fontSize: 16, color: colors.text },
  link: { color: colors.accent, fontSize: 16 },
  inlineLink: { color: colors.accent, textDecorationLine: 'underline' },
});
