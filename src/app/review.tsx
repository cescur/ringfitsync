import { router } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { Alert, Image, Pressable, ScrollView, Share, StyleSheet, Switch, Text, TextInput, View } from 'react-native';

import { Button, Card, colors } from '../components/ui';
import { deleteWorkout, getSettings, saveWorkout, workoutsOnDates, type StoredWorkout } from '../lib/db';
import { deleteWorkoutFromHealth, getHealthStatus, writeWorkout } from '../lib/health';
import {
  formatDuration,
  groupIntoRows,
  hasValues,
  parseDecimalInput,
  parseDurationInput,
  workoutClientId,
  type ParsedWorkout,
} from '../lib/parser';
import { getPendingScan } from '../lib/scanStore';
import { isValidDate, isValidTime, makeWorkout, type Workout } from '../lib/workout';

interface Draft {
  key: string;
  checked: boolean;
  date: string;
  startTime: string;
  duration: string;
  kcal: string;
  km: string;
  dateIsToday?: boolean;
}

function toDraft(w: ParsedWorkout, startTime: string, i: number): Draft {
  return {
    key: `${i}-${w.date}`,
    // Un día con 0:00:00 (sin actividad) no se marca.
    checked: hasValues(w) && w.durationSec !== 0,
    date: w.date,
    startTime,
    duration: w.durationSec === undefined ? '' : formatDuration(w.durationSec),
    kcal: w.kcal === undefined ? '' : String(w.kcal).replace('.', ','),
    km: w.distanceM === undefined ? '' : (w.distanceM / 1000).toFixed(2).replace('.', ','),
    dateIsToday: w.dateIsToday,
  };
}

type Checked = { draft: Draft; workout: Workout } | { draft: Draft; error: string };

function validate(d: Draft): Checked {
  if (!isValidDate(d.date)) return { draft: d, error: 'fecha no válida (AAAA-MM-DD)' };
  if (!isValidTime(d.startTime)) return { draft: d, error: 'hora de inicio no válida (HH:MM)' };
  const durationSec = parseDurationInput(d.duration);
  if (!durationSec) return { draft: d, error: 'falta la duración (H:MM:SS)' };
  const kcal = parseDecimalInput(d.kcal);
  if (kcal === null) return { draft: d, error: 'calorías no válidas' };
  const km = parseDecimalInput(d.km);
  if (km === null) return { draft: d, error: 'distancia no válida' };
  const [hh, mm] = d.startTime.trim().split(':');
  return {
    draft: d,
    workout: makeWorkout({
      date: d.date,
      startTime: `${hh.padStart(2, '0')}:${mm}`,
      durationSec,
      kcal: kcal ?? null,
      distanceM: km === undefined ? null : Math.round(km * 1000),
    }),
  };
}

export default function Review() {
  const scan = useMemo(() => getPendingScan(), []);
  const [drafts, setDrafts] = useState<Draft[]>([]);
  const [existing, setExisting] = useState<StoredWorkout[]>([]);
  const [defaultStart, setDefaultStart] = useState('19:00');
  const [showText, setShowText] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      const settings = await getSettings();
      setDefaultStart(settings.defaultStartTime);
      const parsed = scan?.workouts ?? [];
      const found = await workoutsOnDates(parsed.map((w) => w.date));
      setExisting(found);
      setDrafts(
        parsed.map((w, i) => {
          const d = toDraft(w, settings.defaultStartTime, i);
          // Si ya está guardado tal cual, no se vuelve a marcar.
          const same = w.durationSec !== undefined && found.some((f) => f.id === workoutClientId(w.date, w.durationSec!));
          if (same) d.checked = false;
          // Conserva la hora de inicio que tuviera el registro anterior de ese día.
          const prev = found.find((f) => f.date === w.date);
          if (prev) d.startTime = prev.startTime;
          return d;
        }),
      );
    })();
  }, [scan]);

  const update = (key: string, patch: Partial<Draft>) =>
    setDrafts((ds) => ds.map((d) => (d.key === key ? { ...d, ...patch } : d)));

  const addEmpty = () =>
    setDrafts((ds) => [
      ...ds,
      { key: `manual-${Date.now()}`, checked: true, date: '', startTime: defaultStart, duration: '', kcal: '', km: '' },
    ]);

  const ocrText = useMemo(() => (scan ? groupIntoRows(scan.lines).join('\n') : ''), [scan]);

  const save = async () => {
    const selected = drafts.filter((d) => d.checked).map(validate);
    if (selected.length === 0) {
      Alert.alert('Nada que guardar', 'Marca al menos un día.');
      return;
    }
    const errors = selected.filter((c): c is { draft: Draft; error: string } => 'error' in c);
    if (errors.length > 0) {
      Alert.alert('Revisa los datos', errors.map((e) => `${e.draft.date || 'Día nuevo'}: ${e.error}`).join('\n'));
      return;
    }
    const workouts = selected.map((c) => (c as { workout: Workout }).workout);

    let useHealth = false;
    try {
      useHealth = (await getHealthStatus()) === 'ok';
    } catch {
      useHealth = false;
    }
    if (!useHealth) {
      const go = await new Promise<boolean>((resolve) =>
        Alert.alert(
          'Health Connect sin permisos',
          'No se puede guardar en Health Connect ahora. ¿Guardar solo en el historial de la app?',
          [
            { text: 'Cancelar', style: 'cancel', onPress: () => resolve(false) },
            { text: 'Guardar aquí', onPress: () => resolve(true) },
          ],
        ),
      );
      if (!go) return;
    }

    setSaving(true);
    const failures: string[] = [];
    for (const w of workouts) {
      try {
        // Si ese día ya tenía otro registro (otra duración), se reemplaza.
        for (const old of existing.filter((e) => e.date === w.date && e.id !== w.id)) {
          if (old.inHealthConnect && useHealth) await deleteWorkoutFromHealth(old);
          await deleteWorkout(old.id);
        }
        if (useHealth) await writeWorkout(w);
        await saveWorkout(w, useHealth);
      } catch (e) {
        failures.push(`${w.date}: ${String(e)}`);
      }
    }
    setSaving(false);

    const ok = workouts.length - failures.length;
    Alert.alert(
      failures.length ? 'Guardado con errores' : 'Guardado',
      [
        `${ok} entrenamiento${ok === 1 ? '' : 's'} guardado${ok === 1 ? '' : 's'}${useHealth ? ' en Health Connect y' : ''} en el historial.`,
        ...failures,
      ].join('\n'),
      [{ text: 'Ver historial', onPress: () => router.replace('/history') }],
    );
  };

  if (!scan) {
    return (
      <View style={styles.container}>
        <Text style={styles.muted}>No hay ninguna imagen pendiente de revisar.</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <Image source={{ uri: scan.imageUri }} style={styles.image} resizeMode="contain" />

      {drafts.length === 0 && (
        <Card>
          <Text style={styles.muted}>
            No he encontrado entrenamientos en la imagen. Prueba otra foto con menos reflejos, o añade el día a mano.
          </Text>
        </Card>
      )}

      {drafts.map((d) => {
        const sameSaved = existing.find((e) => d.duration && e.id === workoutClientId(d.date, parseDurationInput(d.duration) ?? -1));
        const other = !sameSaved ? existing.find((e) => e.date === d.date) : undefined;
        return (
          <Card key={d.key} style={!d.checked ? { opacity: 0.6 } : undefined}>
            <View style={styles.rowBetween}>
              <Text style={styles.dayTitle}>{d.date || 'Día nuevo'}</Text>
              <Switch
                value={d.checked}
                onValueChange={(v) => update(d.key, { checked: v })}
                trackColor={{ true: colors.accent }}
              />
            </View>
            {parseDurationInput(d.duration) === 0 && <Text style={styles.note}>Sin actividad este día.</Text>}
            {d.dateIsToday && <Text style={styles.note}>La imagen no tenía fecha: se usa la de hoy.</Text>}
            {sameSaved && <Text style={styles.note}>Ya está guardado.</Text>}
            {other && (
              <Text style={[styles.note, { color: colors.warn }]}>
                Ya hay un registro de este día ({formatDuration(other.durationSec)}). Si lo guardas, se reemplaza.
              </Text>
            )}
            <View style={styles.grid}>
              <Field label="Fecha" value={d.date} onChange={(v) => update(d.key, { date: v })} placeholder="AAAA-MM-DD" />
              <Field label="Hora de inicio" value={d.startTime} onChange={(v) => update(d.key, { startTime: v })} placeholder="19:00" />
              <Field label="Duración" value={d.duration} onChange={(v) => update(d.key, { duration: v })} placeholder="0:20:49" />
              <Field label="Calorías (kcal)" value={d.kcal} onChange={(v) => update(d.key, { kcal: v })} placeholder="74,25" decimal />
              <Field label="Distancia (km)" value={d.km} onChange={(v) => update(d.key, { km: v })} placeholder="1,23" decimal />
            </View>
          </Card>
        );
      })}

      <Button title="Añadir un día a mano" variant="secondary" onPress={addEmpty} />
      <Button title="Guardar seleccionados" onPress={save} loading={saving} />

      <Pressable onPress={() => setShowText((s) => !s)}>
        <Text style={styles.link}>{showText ? 'Ocultar' : 'Ver'} el texto reconocido</Text>
      </Pressable>
      {showText && (
        <Card>
          <Text style={styles.mono}>{ocrText || '(vacío)'}</Text>
          <Button
            title="Compartir texto"
            variant="secondary"
            onPress={() => Share.share({ message: ocrText })}
            style={{ marginTop: 10 }}
          />
        </Card>
      )}
    </ScrollView>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
  decimal,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
  decimal?: boolean;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor="#A0A8B3"
        keyboardType={decimal ? 'decimal-pad' : 'numbers-and-punctuation'}
        style={styles.input}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 14 },
  image: { width: '100%', height: 200, borderRadius: 12, backgroundColor: '#E3E7EC' },
  muted: { fontSize: 15, color: colors.muted, lineHeight: 21 },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  dayTitle: { fontSize: 17, fontWeight: '700', color: colors.text },
  note: { fontSize: 13, color: colors.muted, marginTop: 4 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginTop: 10 },
  field: { width: '47%' },
  label: { fontSize: 12, color: colors.muted, marginBottom: 4 },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    fontSize: 16,
    color: colors.text,
  },
  link: { color: colors.accent, fontSize: 15, textAlign: 'center', paddingVertical: 6 },
  mono: { fontFamily: 'monospace', fontSize: 12, color: colors.text },
});
