import { Ionicons } from '@expo/vector-icons';
import { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { Pressable, StyleSheet, Text, type ViewStyle } from 'react-native';

import { colors } from './ui';

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

function toDate(mode: 'date' | 'time', value: string): Date {
  const now = new Date();
  if (mode === 'time') {
    const [hh, mm] = value.split(':').map(Number);
    if (!Number.isNaN(hh) && !Number.isNaN(mm)) now.setHours(hh, mm, 0, 0);
    return now;
  }
  const [y, m, d] = value.split('-').map(Number);
  return y && m && d ? new Date(y, m - 1, d) : now;
}

function fromDate(mode: 'date' | 'time', d: Date): string {
  return mode === 'time'
    ? `${pad(d.getHours())}:${pad(d.getMinutes())}`
    : `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Campo que abre el selector nativo de Android para una hora (HH:MM) o una fecha (AAAA-MM-DD). */
export function PickerField({
  mode,
  value,
  onChange,
  placeholder,
  title,
  style,
}: {
  mode: 'date' | 'time';
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  title?: string;
  style?: ViewStyle;
}) {
  const open = () =>
    DateTimePickerAndroid.open({
      mode,
      value: toDate(mode, value),
      is24Hour: true,
      design: 'material',
      title,
      maximumDate: mode === 'date' ? new Date() : undefined,
      onChange: (event, date) => {
        if (event.type === 'set' && date) onChange(fromDate(mode, date));
      },
    });

  return (
    <Pressable onPress={open} style={[styles.field, style]} accessibilityRole="button" accessibilityLabel={title}>
      <Text style={[styles.text, !value && { color: '#A0A8B3' }]}>{value || placeholder}</Text>
      <Ionicons name={mode === 'time' ? 'time-outline' : 'calendar-outline'} size={18} color={colors.muted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  field: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    minHeight: 42,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 6,
  },
  text: { fontSize: 16, color: colors.text },
});
