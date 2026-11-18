import type { SavedGameRecord } from '../../types/chess';

/**
 * Pure platform-agnostic deduplication signature for SavedGameRecord.
 * Shared between Web (Dexie/IndexedDB) and Android (expo-sqlite) storage engines.
 */
export function getSavedRecordDedupSignature(record: SavedGameRecord): string {
  const w = (record.metadata?.whiteName || '').trim().toLowerCase();
  const b = (record.metadata?.blackName || '').trim().toLowerCase();
  const d = (record.metadata?.date || '').trim();
  const r = (record.metadata?.result || '').trim();
  const e = (record.metadata?.event || '').trim().toLowerCase();

  const bodyMoves = (record.pgn || '')
    .replace(/\[[^\]]*\]/g, ' ')
    .replace(/\{[^}]*\}/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  if (bodyMoves.length > 10) {
    return `${w}|${b}|${r}|${bodyMoves}`;
  }
  return `${w}|${b}|${d}|${r}|${e}|${record.id}`;
}
