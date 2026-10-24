import { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { Card, colors } from '../components/ui';
import { getSettings, setSetting, type Settings } from '../lib/db';
import { openHealthConnectSettings } from '../lib/health';
import type { DateOrder } from '../lib/parser';
import { isValidTime } from '../lib/workout';

export default function SettingsScreen() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [startTime, setStartTime] = useState('');

  useEffect(() => {
    getSettings().then((s) => {
      setSettings(s);
      setStartTime(s.defaultStartTime);
    });
  }, []);

  if (!settings) return null;

  const setOrder = async (dateOrder: DateOrder) => {
    await setSetting('dateOrder', dateOrder);
    setSettings({ ...settings, dateOrder });
  };

  const saveStart = async () => {
    const t = startTime.trim();
    if (!isValidTime(t)) {
      Alert.alert('Hora no válida', 'Usa el formato HH:MM, por ejemplo 19:00.');
      setStartTime(settings.defaultStartTime);
      return;
    }
    const [hh, mm] = t.split(':');
    const value = `${hh.padStart(2, '0')}:${mm}`;
    await setSetting('defaultStartTime', value);
    setSettings({ ...settings, defaultStartTime: value });
    setStartTime(value);
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
        <TextInput
          value={startTime}
          onChangeText={setStartTime}
          onEndEditing={saveStart}
          placeholder="19:00"
          keyboardType="numbers-and-punctuation"
          style={styles.input}
        />
      </Card>

      <Card>
        <Pressable onPress={openHealthConnectSettings}>
          <Text style={styles.link}>Abrir ajustes de Health Connect</Text>
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
  input: {
    marginTop: 10,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    fontSize: 16,
    color: colors.text,
    width: 100,
  },
  link: { color: colors.accent, fontSize: 16 },
});
