import Dexie, { type Table } from 'dexie';
import type { SavedGameRecord, AppSettings } from '../../types/chess';
import type {
  OpeningAnalysisCacheRecord,
  OpeningRepertoireCacheRecord,
  PersistedRepertoireTaskState,
} from '../../types/repertoire';
import { getSavedRecordDedupSignature } from './dedupUtils';

export { getSavedRecordDedupSignature };

export interface StoredSettingRecord {
  key: string;
  value: AppSettings;
}

export class ChessAnalyzerDatabase extends Dexie {
  games!: Table<SavedGameRecord, string>;
  settings!: Table<StoredSettingRecord, string>;
  openingAnalysisCache!: Table<OpeningAnalysisCacheRecord, string>;
  openingRepertoireCache!: Table<OpeningRepertoireCacheRecord, string>;
  repertoireTaskState!: Table<PersistedRepertoireTaskState, string>;

  constructor(dbName = 'ChessInsightAnalyzerDB_v3') {
    super(dbName);
    this.version(1).stores({
      games: 'id, updatedAt, createdAt, metadata.whiteName, metadata.blackName, metadata.platform',
      settings: 'key',
      openingAnalysisCache:
        'id, gameId, playerUsername, playerColor, eco, openingName, timeClass, timestamp, status, pgnHash',
      openingRepertoireCache: 'id, playerIdentifier, createdAt',
      repertoireTaskState: 'id, updatedAt, status',
    });
  }
}

export let db = new ChessAnalyzerDatabase();

let openCheckPromise: Promise<void> | null = null;

async function ensureDbReady(): Promise<ChessAnalyzerDatabase> {
  if (db.isOpen()) return db;
  if (!openCheckPromise) {
    openCheckPromise = db
      .open()
      .then(() => {})
      .catch(async (err) => {
        // Automatically recover if an older browser IndexedDB schema had a different primary key
        if (err && (err.name === 'UpgradeError' || err.name === 'VersionError' || err.name === 'DatabaseClosedError')) {
          try {
            await db.delete();
            db = new ChessAnalyzerDatabase();
            await db.open();
          } catch {
            // ignore fallback errors
          }
        }
      })
      .finally(() => {
        openCheckPromise = null;
      });
  }
  await openCheckPromise;
  return db;
}

export async function saveGameToIndexedDB(record: SavedGameRecord): Promise<void> {
  const database = await ensureDbReady();
  const allExisting = await database.games.toArray();
  const targetSig = getSavedRecordDedupSignature(record);

  const duplicateIdsToDelete: string[] = [];
  for (const item of allExisting) {
    if (item.id !== record.id && getSavedRecordDedupSignature(item) === targetSig) {
      duplicateIdsToDelete.push(item.id);
    }
  }

  if (duplicateIdsToDelete.length > 0) {
    await database.games.bulkDelete(duplicateIdsToDelete);
  }

  await database.games.put(record);
}

export async function loadAllSavedGames(): Promise<SavedGameRecord[]> {
  const database = await ensureDbReady();
  const rawRecords = await database.games.orderBy('updatedAt').reverse().toArray();
  const seenSignatures = new Set<string>();
  const uniqueRecords: SavedGameRecord[] = [];
  const duplicateIds: string[] = [];

  for (const rec of rawRecords) {
    const sig = getSavedRecordDedupSignature(rec);
    if (seenSignatures.has(sig)) {
      duplicateIds.push(rec.id);
    } else {
      seenSignatures.add(sig);
      uniqueRecords.push(rec);
    }
  }

  if (duplicateIds.length > 0) {
    database.games.bulkDelete(duplicateIds).catch(() => {});
  }

  return uniqueRecords;
}

export async function deleteSavedGame(id: string): Promise<void> {
  const database = await ensureDbReady();
  await database.games.delete(id);
}

export async function saveSettingsToIndexedDB(settings: AppSettings): Promise<void> {
  const database = await ensureDbReady();
  await database.settings.put({ key: 'user_settings', value: settings });
}

export async function loadSettingsFromIndexedDB(): Promise<AppSettings | null> {
  const database = await ensureDbReady();
  const rec = await database.settings.get('user_settings');
  return rec ? rec.value : null;
}

export async function getOpeningAnalysisCacheItem(
  id: string
): Promise<OpeningAnalysisCacheRecord | undefined> {
  const database = await ensureDbReady();
  return database.openingAnalysisCache.get(id);
}

export async function getAllOpeningAnalysisCacheItems(): Promise<OpeningAnalysisCacheRecord[]> {
  const database = await ensureDbReady();
  return database.openingAnalysisCache.toArray();
}

export async function getOpeningAnalysisCacheCount(): Promise<number> {
  const database = await ensureDbReady();
  return database.openingAnalysisCache.count();
}

export async function putOpeningAnalysisCacheItem(
  record: OpeningAnalysisCacheRecord
): Promise<void> {
  const database = await ensureDbReady();
  await database.openingAnalysisCache.put(record);
}

export async function bulkPutOpeningAnalysisCacheItems(
  records: OpeningAnalysisCacheRecord[]
): Promise<void> {
  if (records.length === 0) return;
  const database = await ensureDbReady();
  await database.openingAnalysisCache.bulkPut(records);
}

export async function getOpeningRepertoireCacheItem(
  id: string
): Promise<OpeningRepertoireCacheRecord | undefined> {
  const database = await ensureDbReady();
  return database.openingRepertoireCache.get(id);
}

export async function putOpeningRepertoireCacheItem(
  snapshot: OpeningRepertoireCacheRecord
): Promise<void> {
  const database = await ensureDbReady();
  await database.openingRepertoireCache.put(snapshot);
}

export async function getAllOpeningRepertoireCacheItems(): Promise<OpeningRepertoireCacheRecord[]> {
  const database = await ensureDbReady();
  return database.openingRepertoireCache.orderBy('createdAt').reverse().toArray();
}

export async function getPersistedRepertoireTask(): Promise<
  PersistedRepertoireTaskState | undefined
> {
  const database = await ensureDbReady();
  return database.repertoireTaskState.get('active_repertoire_task');
}

export async function putPersistedRepertoireTask(
  task: PersistedRepertoireTaskState
): Promise<void> {
  const database = await ensureDbReady();
  await database.repertoireTaskState.put(task);
}

export async function clearPersistedRepertoireTask(): Promise<void> {
  const database = await ensureDbReady();
  await database.repertoireTaskState.delete('active_repertoire_task');
}

export async function deleteOpeningAnalysisCacheByIds(ids: string[]): Promise<void> {
  if (ids.length === 0) return;
  const database = await ensureDbReady();
  await database.openingAnalysisCache.bulkDelete(ids);
}

export async function deleteOpeningAnalysisCacheByPlayer(playerUsername: string): Promise<void> {
  const normalized = playerUsername.trim().toLowerCase();
  if (!normalized) return;
  const database = await ensureDbReady();
  const all = await database.openingAnalysisCache.toArray();
  const matchingIds = all
    .filter((r) => (r.playerUsername || '').trim().toLowerCase() === normalized)
    .map((r) => r.id);
  if (matchingIds.length > 0) {
    await database.openingAnalysisCache.bulkDelete(matchingIds);
  }
}

export async function clearAllOpeningRepertoireCaches(): Promise<void> {
  const database = await ensureDbReady();
  await database.transaction(
    'rw',
    database.openingAnalysisCache,
    database.openingRepertoireCache,
    database.repertoireTaskState,
    async () => {
      await database.openingAnalysisCache.clear();
      await database.openingRepertoireCache.clear();
      await database.repertoireTaskState.clear();
    }
  );
}
