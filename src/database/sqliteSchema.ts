import {
  getOpeningAnalysisCacheCount,
  getSavedRecordDedupSignature,
  loadAllSavedGames,
  saveGameToIndexedDB,
} from '../services/StorageService/db';
import type { SavedGameRecord } from '../types/chess';

/**
 * SQLite Schema v3 for React Native (`expo-sqlite`) migration.
 * Mirrors the 5 tables from Dexie IndexedDB v3 plus a dedicated `user_activity_log` table.
 */
export const SQLITE_DATABASE_NAME = 'chess_insight_analyzer_v3.db';
export const SQLITE_SCHEMA_VERSION = 3;

export const SQLITE_DDL_STATEMENTS: string[] = [
  `CREATE TABLE IF NOT EXISTS games (
    id TEXT PRIMARY KEY NOT NULL,
    updated_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    white_name TEXT NOT NULL,
    black_name TEXT NOT NULL,
    platform TEXT NOT NULL,
    pgn TEXT NOT NULL,
    initial_fen TEXT NOT NULL,
    serialized_tree TEXT NOT NULL,
    metadata_json TEXT NOT NULL,
    statistics_json TEXT,
    opening_summary_json TEXT
  );`,
  `CREATE INDEX IF NOT EXISTS idx_games_updated_at ON games(updated_at DESC);`,
  `CREATE INDEX IF NOT EXISTS idx_games_white_name ON games(white_name);`,
  `CREATE INDEX IF NOT EXISTS idx_games_black_name ON games(black_name);`,

  `CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY NOT NULL,
    value_json TEXT NOT NULL,
    updated_at INTEGER NOT NULL
  );`,

  `CREATE TABLE IF NOT EXISTS opening_analysis_cache (
    id TEXT PRIMARY KEY NOT NULL,
    game_id TEXT NOT NULL,
    player_username TEXT NOT NULL,
    player_color TEXT NOT NULL,
    eco TEXT NOT NULL,
    opening_name TEXT NOT NULL,
    time_class TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    status TEXT NOT NULL,
    pgn_hash TEXT NOT NULL,
    payload_json TEXT NOT NULL
  );`,
  `CREATE INDEX IF NOT EXISTS idx_opening_cache_player ON opening_analysis_cache(player_username);`,
  `CREATE INDEX IF NOT EXISTS idx_opening_cache_timestamp ON opening_analysis_cache(timestamp DESC);`,
  `CREATE INDEX IF NOT EXISTS idx_opening_cache_eco ON opening_analysis_cache(eco);`,

  `CREATE TABLE IF NOT EXISTS opening_repertoire_cache (
    id TEXT PRIMARY KEY NOT NULL,
    player_identifier TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    snapshot_json TEXT NOT NULL
  );`,
  `CREATE INDEX IF NOT EXISTS idx_repertoire_created_at ON opening_repertoire_cache(created_at DESC);`,

  `CREATE TABLE IF NOT EXISTS repertoire_task_state (
    id TEXT PRIMARY KEY NOT NULL,
    status TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    state_json TEXT NOT NULL
  );`,

  `CREATE TABLE IF NOT EXISTS user_activity_log (
    id TEXT PRIMARY KEY NOT NULL,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    subtitle TEXT,
    timestamp INTEGER NOT NULL,
    game_id TEXT
  );`,
  `CREATE INDEX IF NOT EXISTS idx_activity_timestamp ON user_activity_log(timestamp DESC);`,
];

export const SQLITE_MIGRATION_DDL_V1_TO_V3: string[] = SQLITE_DDL_STATEMENTS;

export type ActivityEventType =
  | 'import_pgn'
  | 'analyze_game'
  | 'sync_chesscom'
  | 'export_pgn'
  | 'open_game'
  | 'repertoire_build'
  | 'IMPORT_PGN'
  | 'ANALYZE_GAME'
  | 'SYNC_CHESSCOM'
  | 'EXPORT_SNAPSHOT'
  | 'RESTORE_SNAPSHOT'
  | 'CLEAR_CACHE';

export interface UserActivityEvent {
  id: string;
  type: ActivityEventType;
  eventType?: ActivityEventType;
  title: string;
  subtitle: string;
  timestamp: number;
  createdAt?: number;
  gameId?: string;
}

const ACTIVITY_STORAGE_KEY = 'chess_analyzer_activity_log_v1';

const DEFAULT_ACTIVITY_EVENTS: UserActivityEvent[] = [
  {
    id: 'seed_1',
    type: 'open_game',
    title: 'Готовность базы данных SQLite v3 / IndexedDB',
    subtitle: 'Локальный архив партий и книга дебютов ECO инициализированы',
    timestamp: Date.now() - 120_000,
  },
];

const memoryActivityFallback: UserActivityEvent[] = [...DEFAULT_ACTIVITY_EVENTS];

function hasBrowserLocalStorage(): boolean {
  try {
    return typeof localStorage !== 'undefined' && localStorage !== null;
  } catch {
    return false;
  }
}

export async function ensureDatabaseMigrated(): Promise<void> {
  // Ensures storage adapter is initialized and activity log is seeded
  await loadAllSavedGames();
}

export function loadUserActivityLog(): UserActivityEvent[] {
  if (!hasBrowserLocalStorage()) {
    return [...memoryActivityFallback];
  }
  try {
    const raw = localStorage.getItem(ACTIVITY_STORAGE_KEY);
    if (!raw) return [...DEFAULT_ACTIVITY_EVENTS];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : [...DEFAULT_ACTIVITY_EVENTS];
  } catch {
    return [...DEFAULT_ACTIVITY_EVENTS];
  }
}

export function getRecentActivities(): UserActivityEvent[] {
  return loadUserActivityLog();
}

export function logUserActivity(
  inputOrType:
    | {
        type: ActivityEventType;
        title: string;
        subtitle?: string;
        gameId?: string;
      }
    | ActivityEventType,
  maybeTitle?: string,
  maybeSubtitle?: string
): UserActivityEvent[] {
  const existing = loadUserActivityLog();
  const now = Date.now();

  const normalized: UserActivityEvent =
    typeof inputOrType === 'object'
      ? {
          id: `${now}-${Math.random().toString(36).slice(2, 7)}`,
          type: inputOrType.type,
          eventType: inputOrType.type,
          title: inputOrType.title,
          subtitle: inputOrType.subtitle || '',
          timestamp: now,
          createdAt: now,
          gameId: inputOrType.gameId,
        }
      : {
          id: `${now}-${Math.random().toString(36).slice(2, 7)}`,
          type: inputOrType,
          eventType: inputOrType,
          title: maybeTitle || '',
          subtitle: maybeSubtitle || '',
          timestamp: now,
          createdAt: now,
        };

  const updated = [normalized, ...existing].slice(0, 40);
  if (hasBrowserLocalStorage()) {
    try {
      localStorage.setItem(ACTIVITY_STORAGE_KEY, JSON.stringify(updated));
    } catch {
      // ignore storage quota errors
    }
  } else {
    memoryActivityFallback.splice(0, memoryActivityFallback.length, ...updated);
  }
  return updated;
}

export async function batchImportPgnGamesToDb(
  pgnList: string[],
  buildRecord: (pgn: string) => SavedGameRecord
): Promise<{
  imported: number;
  skippedDuplicates: number;
  firstRecord: SavedGameRecord | null;
}> {
  const existingGames = await loadAllSavedGames();
  const existingSignatures = new Set(existingGames.map((g) => getSavedRecordDedupSignature(g)));

  let imported = 0;
  let skippedDuplicates = 0;
  let firstRecord: SavedGameRecord | null = null;

  for (const rawPgn of pgnList) {
    try {
      const record = buildRecord(rawPgn);
      const sig = getSavedRecordDedupSignature(record);
      if (existingSignatures.has(sig)) {
        skippedDuplicates++;
        continue;
      }
      existingSignatures.add(sig);
      await saveGameToIndexedDB(record);
      if (!firstRecord) {
        firstRecord = record;
      }
      imported++;
    } catch {
      // skip invalid PGN
    }
  }

  return { imported, skippedDuplicates, firstRecord };
}

export interface PortableDatabaseBackup {
  schemaVersion: number;
  exportedAt: number;
  platform: 'react-native-sqlite-compatible';
  gamesCount: number;
  openingCacheCount: number;
  games: SavedGameRecord[];
  activityLog: UserActivityEvent[];
}

export async function exportPortableBackup(): Promise<PortableDatabaseBackup> {
  const games = await loadAllSavedGames();
  const openingCacheCount = await getOpeningAnalysisCacheCount();
  const activityLog = loadUserActivityLog();

  logUserActivity(
    'EXPORT_SNAPSHOT',
    `Экспорт резервной копии БД (${games.length} партий)`,
    `Схема SQLite v${SQLITE_SCHEMA_VERSION}`
  );

  return {
    schemaVersion: SQLITE_SCHEMA_VERSION,
    exportedAt: Date.now(),
    platform: 'react-native-sqlite-compatible',
    gamesCount: games.length,
    openingCacheCount,
    games,
    activityLog,
  };
}

export async function importPortableBackup(jsonText: string): Promise<{ restoredGames: number }> {
  const parsed = JSON.parse(jsonText) as Partial<PortableDatabaseBackup>;
  if (!parsed || !Array.isArray(parsed.games)) {
    throw new Error('Некорректный формат резервной копии базы данных.');
  }

  let restoredGames = 0;
  for (const game of parsed.games) {
    if (game && typeof game.id === 'string' && typeof game.pgn === 'string') {
      await saveGameToIndexedDB(game);
      restoredGames++;
    }
  }

  logUserActivity(
    'RESTORE_SNAPSHOT',
    `Восстановлено партий из бэкапа: ${restoredGames}`,
    `Дата снимка: ${parsed.exportedAt ? new Date(parsed.exportedAt).toLocaleDateString('ru-RU') : 'неизвестно'}`
  );

  return { restoredGames };
}
