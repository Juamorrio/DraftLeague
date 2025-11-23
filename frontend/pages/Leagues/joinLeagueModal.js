import React from 'react';
import { Modal, View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native';

export default function JoinLeagueModal({ visible, onClose, code, setCode, onJoin, loading, error }) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <Text style={styles.title}>Unirse a liga</Text>
          <Text style={styles.label}>Código</Text>
          <TextInput
            style={styles.input}
            placeholder="ABC123"
            autoCapitalize="characters"
            value={code}
            onChangeText={t => setCode(t.toUpperCase())}
            maxLength={6}
          />
          {error ? <Text style={styles.error}>{error}</Text> : null}
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
  backdrop: { flex:1, backgroundColor:'rgba(0,0,0,0.35)', justifyContent:'center', alignItems:'center', padding:16 },
  card: { width:'92%', maxWidth:400, backgroundColor:'#1f2937', borderRadius:18, padding:20 },
  title: { fontSize:20, fontWeight:'700', color:'#fff', marginBottom:12, textAlign:'center' },
  label: { color:'#e2e8f0', marginBottom:6, fontWeight:'600' },
  input: { backgroundColor:'#fff', borderRadius:12, paddingHorizontal:12, height:44, fontSize:16, letterSpacing:2, color:'#111827' },
  actions: { flexDirection:'row', justifyContent:'flex-end', marginTop:16, gap:10 },
  btn: { paddingHorizontal:16, paddingVertical:10, borderRadius:10 },
  cancel: { backgroundColor:'#6b7280' },
  join: { backgroundColor:'#2563eb' },
  disabled: { backgroundColor:'#94a3b8' },
  btnTxt: { color:'#fff', fontWeight:'600' },
  error: { marginTop:10, backgroundColor:'rgba(239,68,68,0.25)', color:'#fecaca', padding:8, borderRadius:10, fontWeight:'600' }
});
