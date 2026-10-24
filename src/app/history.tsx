import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { Button, Card, colors } from '../components/ui';
import { deleteWorkout, listWorkouts, type StoredWorkout } from '../lib/db';
import { deleteWorkoutFromHealth } from '../lib/health';
import { formatDuration } from '../lib/parser';
import { monthlyStats, toCsv, weeklyStats, type PeriodStats } from '../lib/stats';

type Tab = 'list' | 'weeks' | 'months';

function km(m: number | null): string {
  return m === null ? '–' : `${(m / 1000).toFixed(2).replace('.', ',')} km`;
}

function kcal(v: number | null): string {
  return v === null ? '–' : `${v.toFixed(1).replace('.', ',')} kcal`;
}

export default function History() {
  const [workouts, setWorkouts] = useState<StoredWorkout[]>([]);
  const [tab, setTab] = useState<Tab>('list');

  const load = useCallback(() => {
    listWorkouts().then(setWorkouts);
  }, []);
  useFocusEffect(load);

  const weeks = useMemo(() => weeklyStats(workouts), [workouts]);
  const months = useMemo(() => monthlyStats(workouts), [workouts]);

  const remove = (w: StoredWorkout) =>
    Alert.alert('Borrar entrenamiento', `¿Borrar el del ${w.date}? También se borra de Health Connect.`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Borrar',
        style: 'destructive',
        onPress: async () => {
          try {
            if (w.inHealthConnect) await deleteWorkoutFromHealth(w);
          } catch (e) {
            Alert.alert('Health Connect', `No se pudo borrar de Health Connect: ${String(e)}`);
            return;
          }
          await deleteWorkout(w.id);
          load();
        },
      },
    ]);

  const exportCsv = async () => {
    try {
      const file = new File(Paths.cache, `ringfit-${new Date().toISOString().slice(0, 10)}.csv`);
      if (file.exists) file.delete();
      file.create();
      file.write(toCsv(workouts));
      await Sharing.shareAsync(file.uri, { mimeType: 'text/csv', dialogTitle: 'Exportar CSV' });
    } catch (e) {
      Alert.alert('No se pudo exportar', String(e));
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.tabs}>
        {(
          [
            ['list', 'Entrenamientos'],
            ['weeks', 'Semanas'],
            ['months', 'Meses'],
          ] as const
        ).map(([key, label]) => (
          <Pressable key={key} onPress={() => setTab(key)} style={[styles.tab, tab === key && styles.tabActive]}>
            <Text style={[styles.tabText, tab === key && styles.tabTextActive]}>{label}</Text>
          </Pressable>
        ))}
      </View>

      {workouts.length === 0 && <Text style={styles.muted}>Todavía no hay entrenamientos guardados.</Text>}

      {tab === 'list' &&
        workouts.map((w) => (
          <Card key={w.id}>
            <View style={styles.rowBetween}>
              <Text style={styles.title}>
                {w.date} · {w.startTime}
              </Text>
              <Pressable onPress={() => remove(w)} hitSlop={10}>
                <Text style={{ color: colors.danger, fontSize: 14 }}>Borrar</Text>
              </Pressable>
            </View>
            <Text style={styles.values}>
              {formatDuration(w.durationSec)} · {kcal(w.kcal)} · {km(w.distanceM)}
            </Text>
            {!w.inHealthConnect && <Text style={styles.note}>Solo en el historial (no está en Health Connect).</Text>}
          </Card>
        ))}

      {tab === 'weeks' && weeks.map((p) => <PeriodCard key={p.key} p={p} />)}
      {tab === 'months' && months.map((p) => <PeriodCard key={p.key} p={p} />)}

      {workouts.length > 0 && <Button title="Exportar todo a CSV" variant="secondary" onPress={exportCsv} />}
    </ScrollView>
  );
}

function PeriodCard({ p }: { p: PeriodStats }) {
  return (
    <Card>
      <Text style={styles.title}>{p.label}</Text>
      <Text style={styles.note}>
        {p.sessions} día{p.sessions === 1 ? '' : 's'} de ejercicio
      </Text>
      <Text style={styles.values}>
        Total: {formatDuration(p.totalSec)} · {kcal(p.totalKcal)} · {km(p.totalM)}
      </Text>
      <Text style={styles.values}>
        Media: {formatDuration(Math.round(p.avgSec))} · {kcal(p.avgKcal)} · {km(p.avgM)}
      </Text>
    </Card>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 12 },
  tabs: { flexDirection: 'row', backgroundColor: '#E9ECF0', borderRadius: 10, padding: 3 },
  tab: { flex: 1, paddingVertical: 8, borderRadius: 8, alignItems: 'center' },
  tabActive: { backgroundColor: colors.card },
  tabText: { color: colors.muted, fontSize: 14, fontWeight: '600' },
  tabTextActive: { color: colors.text },
  muted: { fontSize: 15, color: colors.muted },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  title: { fontSize: 16, fontWeight: '700', color: colors.text },
  values: { fontSize: 15, color: colors.text, marginTop: 4 },
  note: { fontSize: 13, color: colors.muted, marginTop: 4 },
});
