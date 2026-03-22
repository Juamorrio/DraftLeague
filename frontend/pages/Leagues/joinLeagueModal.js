import React from 'react';
import { Modal, View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';
import { colors, fontSize, fontWeight, radius, spacing, shadow } from '../../utils/theme';

export default function JoinLeagueModal({ visible, onClose, code, setCode, onJoin, loading, error }) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <Text style={styles.title}>Unirse a liga</Text>
          <Text style={styles.label}>Código</Text>
          <TextInput
            style={[styles.input, error ? styles.inputError : null]}
            placeholder="ABC123"
            autoCapitalize="characters"
            value={code}
            onChangeText={t => setCode(t.toUpperCase())}
            maxLength={6}
          />
          {error ? <Text style={styles.errorText}>{error}</Text> : null}
          <View style={styles.actions}>
            <TouchableOpacity style={[styles.btn, styles.cancel]} onPress={onClose} disabled={loading}>
              <Text style={styles.btnTxt}>Cancelar</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.btn, !code || code.length!==6 ? styles.disabled : styles.join]} onPress={onJoin} disabled={loading || code.length!==6}>
              {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnTxt}>Unirse</Text>}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: colors.overlay, justifyContent: 'center', alignItems: 'center', padding: spacing.lg },
  card: { width: '92%', maxWidth: 400, backgroundColor: colors.bgCard, borderRadius: radius.xl, padding: spacing.xl, ...shadow.lg },
  title: { fontSize: fontSize.xl, fontWeight: fontWeight.bold, color: colors.textPrimary, marginBottom: spacing.md, textAlign: 'center' },
  label: { color: colors.textSecondary, marginBottom: 6, fontWeight: fontWeight.semibold },
  input: {
    backgroundColor: colors.bgSubtle, borderRadius: radius.md, paddingHorizontal: spacing.md,
    height: 48, fontSize: fontSize.md, letterSpacing: 2, color: colors.textPrimary,
    borderWidth: 1.5, borderColor: colors.border,
  },
  inputError: { borderColor: colors.danger },
  actions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: spacing.lg, gap: spacing.sm },
  btn: { paddingHorizontal: spacing.lg, paddingVertical: spacing.sm, borderRadius: radius.md },
  cancel: { backgroundColor: colors.textSecondary },
  join: { backgroundColor: colors.primary },
  disabled: { backgroundColor: colors.textMuted },
  btnTxt: { color: colors.textInverse, fontWeight: fontWeight.semibold },
  errorText: { color: colors.danger, marginTop: spacing.sm, fontSize: fontSize.sm, fontWeight: fontWeight.semibold },
});
