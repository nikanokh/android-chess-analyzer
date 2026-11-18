import React, { useMemo, useRef, useState } from 'react';
import { useGameStore } from '../../stores/useGameStore';
import { MainTabId } from '../../theme';
import { SavedGameRecord } from '../../types/chess';
import {
  BottomSheetModal,
  SegmentedControl,
  TouchButton,
} from '../../components/ui/MobilePrimitives';
import {
  ParsedPgnPreviewItem,
  parseMultiGamePgnPreview,
} from '../../chess/capturedPieces';
import { batchImportPgnGamesToDb, logUserActivity } from '../../database/sqliteSchema';
import { parsePgnToTree } from '../../services/PgnService/pgnService';
import { getMainlineNodes } from '../../core/chess/VariationTree';
import { openingBook } from '../../core/chess/OpeningBook';
import {
  ArrowUpDown,
  CheckCircle2,
  Database,
  Download,
  FileUp,
  FolderOpen,
  Globe,
  PlusCircle,
  Search,
  Share2,
  Trash2,
  Trophy,
  Upload,
} from 'lucide-react';

interface GamesArchiveScreenProps {
  onNavigateTab: (tab: MainTabId) => void;
  onOpenChessComModal: () => void;
  onOpenImportModal: () => void;
}

type ArchiveSortField = 'updatedAt' | 'accuracy' | 'rating' | 'date';
type AnalysisFilter = 'all' | 'analyzed' | 'unanalyzed';
type PlatformFilter = 'all' | 'Chess.com' | 'PGN' | 'Analysis';

const PAGE_WINDOW_SIZE = 40;

export const GamesArchiveScreen: React.FC<GamesArchiveScreenProps> = ({
  onNavigateTab,
  onOpenChessComModal,
  onOpenImportModal,
}) => {
  const savedGames = useGameStore((s) => s.savedGames);
  const currentGameId = useGameStore((s) => s.gameId);
  const loadSavedGameRecord = useGameStore((s) => s.loadSavedGameRecord);
  const removeSavedGame = useGameStore((s) => s.removeSavedGame);
  const refreshSavedGames = useGameStore((s) => s.refreshSavedGames);
  const startNewGame = useGameStore((s) => s.startNewGame);

  // Filters & Sorting
  const [searchQuery, setSearchQuery] = useState('');
  const [platformFilter, setPlatformFilter] = useState<PlatformFilter>('all');
  const [analysisFilter, setAnalysisFilter] = useState<AnalysisFilter>('all');
  const [sortField, setSortField] = useState<ArchiveSortField>('updatedAt');
  const [visibleLimit, setVisibleLimit] = useState(PAGE_WINDOW_SIZE);

  // Multi-game PGN file preview state
  const [isPreviewSheetOpen, setIsPreviewSheetOpen] = useState(false);
  const [previewItems, setPreviewItems] = useState<ParsedPgnPreviewItem[]>([]);
  const [selectedPreviewIndices, setSelectedPreviewIndices] = useState<Set<number>>(new Set());
  const [isImportingBatch, setIsImportingBatch] = useState(false);
  const [statusBanner, setStatusBanner] = useState<string | null>(null);

  const filePickerRef = useRef<HTMLInputElement | null>(null);

  const filteredAndSortedGames = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();

    const filtered = savedGames.filter((rec) => {
      if (platformFilter !== 'all' && rec.metadata.platform !== platformFilter) {
        return false;
      }
      if (analysisFilter === 'analyzed' && !rec.statistics) {
        return false;
      }
      if (analysisFilter === 'unanalyzed' && Boolean(rec.statistics)) {
        return false;
      }
      if (q) {
        const hay = [
          rec.metadata.whiteName,
          rec.metadata.blackName,
          rec.metadata.eco,
          rec.metadata.openingName,
          rec.openingSummary?.eco,
          rec.openingSummary?.name,
          rec.metadata.date,
          rec.metadata.event,
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });

    return filtered.sort((a, b) => {
      if (sortField === 'accuracy') {
        const accA = a.statistics
          ? (a.statistics.whiteAccuracy + a.statistics.blackAccuracy) / 2
          : -1;
        const accB = b.statistics
          ? (b.statistics.whiteAccuracy + b.statistics.blackAccuracy) / 2
          : -1;
        return accB - accA;
      }
      if (sortField === 'rating') {
        const rA = Math.max(a.metadata.whiteRating || 0, a.metadata.blackRating || 0);
        const rB = Math.max(b.metadata.whiteRating || 0, b.metadata.blackRating || 0);
        return rB - rA;
      }
      if (sortField === 'date') {
        return (b.metadata.date || '').localeCompare(a.metadata.date || '');
      }
      return b.updatedAt - a.updatedAt;
    });
  }, [savedGames, searchQuery, platformFilter, analysisFilter, sortField]);

  const visibleGames = useMemo(
    () => filteredAndSortedGames.slice(0, visibleLimit),
    [filteredAndSortedGames, visibleLimit]
  );

  // System file picker handler for multi-game PGN import with preview
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const content = typeof ev.target?.result === 'string' ? ev.target.result : '';
      const parsed = parseMultiGamePgnPreview(content);
      setPreviewItems(parsed);
      const validSet = new Set<number>();
      for (const item of parsed) {
        if (item.isValid) validSet.add(item.index);
      }
      setSelectedPreviewIndices(validSet);
      setIsPreviewSheetOpen(true);
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const handleConfirmBatchImport = async () => {
    const chosenPgns = previewItems
      .filter((item) => item.isValid && selectedPreviewIndices.has(item.index))
      .map((item) => item.rawPgn);

    if (chosenPgns.length === 0) return;

    setIsImportingBatch(true);
    try {
      const result = await batchImportPgnGamesToDb(chosenPgns, (pgn) => {
        const parsed = parsePgnToTree(pgn, 'PGN');
        const mainline = getMainlineNodes(parsed.root);
        const openingSummary = openingBook.analyzeMainlineOpening(mainline);
        const now = Date.now();
        return {
          id: parsed.metadata.id,
          metadata: {
            ...parsed.metadata,
            eco: openingSummary.eco,
            openingName: `${openingSummary.name}: ${openingSummary.variation}`,
          },
          pgn,
          initialFen: parsed.initialFen,
          serializedTree: JSON.stringify(parsed.root),
          openingSummary,
          createdAt: now,
          updatedAt: now,
        };
      });

      await refreshSavedGames();
      if (result.firstRecord) {
        loadSavedGameRecord(result.firstRecord);
      }

      logUserActivity({
        type: 'import_pgn',
        title: `Импортировано партий PGN: ${result.imported}`,
        subtitle:
          result.skippedDuplicates > 0
            ? `Пропущено дубликатов: ${result.skippedDuplicates}`
            : 'Сохранено в локальную базу SQLite',
      });

      setStatusBanner(
        `Успешно импортировано партий: ${result.imported}${
          result.skippedDuplicates > 0
            ? ` (пропущено дубликатов: ${result.skippedDuplicates})`
            : ''
        }`
      );
      setIsPreviewSheetOpen(false);
    } finally {
      setIsImportingBatch(false);
    }
  };

  // Share or Export PGN (Android Share Sheet / File Download)
  const handleShareOrExportPgn = async (rec?: SavedGameRecord) => {
    const pgnContent = rec
      ? rec.pgn
      : filteredAndSortedGames.map((g) => g.pgn).join('\n\n');
    const filename = rec
      ? `${rec.metadata.whiteName}_vs_${rec.metadata.blackName}.pgn`.replace(/\s+/g, '_')
      : `chess_archive_${filteredAndSortedGames.length}_games.pgn`;

    logUserActivity({
      type: 'export_pgn',
      title: rec
        ? `Экспорт PGN: ${rec.metadata.whiteName} vs ${rec.metadata.blackName}`
        : `Экспорт архива (${filteredAndSortedGames.length} партий)`,
      subtitle: filename,
    });

    // Try Android / Mobile Web Share API first if available
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        const file = new File([pgnContent], filename, { type: 'application/x-chess-pgn' });
        if (navigator.canShare && navigator.canShare({ files: [file] })) {
          await navigator.share({
            title: filename,
            text: 'Экспорт шахматной партии PGN',
            files: [file],
          });
          return;
        }
        await navigator.share({
          title: filename,
          text: pgnContent,
        });
        return;
      } catch {
        // Fallback to direct download if user cancels share dialog or unsupported
      }
    }

    const blob = new Blob([pgnContent], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    setStatusBanner(`Файл ${filename} сохранён.`);
  };

  return (
    <div className="flex-1 min-h-0 flex flex-col max-w-6xl w-full mx-auto p-3 sm:p-5 gap-3.5 overflow-hidden">
      {/* Hidden system file input for .pgn */}
      <input
        ref={filePickerRef}
        type="file"
        accept=".pgn,.txt"
        onChange={handleFileChange}
        className="hidden"
      />

      {/* Top Action Header */}
      <div className="p-3.5 sm:p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] flex flex-col sm:flex-row sm:items-center justify-between gap-3 shrink-0">
        <div>
          <h1 className="text-base sm:text-lg font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
            <Database className="w-5 h-5 text-[#779954]" />
            <span>Архив партий и PGN ({filteredAndSortedGames.length})</span>
          </h1>
          <p className="text-xs text-[var(--chess-text-secondary)] mt-0.5">
            Поиск, фильтрация, пакетный импорт/экспорт .pgn и сохранённые оценки Stockfish
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <TouchButton
            variant="primary"
            size="sm"
            leftIcon={<Upload className="w-3.5 h-3.5" />}
            onClick={() => filePickerRef.current?.click()}
          >
            Выбрать .PGN файл
          </TouchButton>

          <TouchButton
            variant="secondary"
            size="sm"
            leftIcon={<FileUp className="w-3.5 h-3.5 text-[#F39C36]" />}
            onClick={onOpenImportModal}
          >
            Вставить PGN / FEN
          </TouchButton>

          <TouchButton
            variant="secondary"
            size="sm"
            leftIcon={<Globe className="w-3.5 h-3.5 text-[#779954]" />}
            onClick={onOpenChessComModal}
          >
            Chess.com
          </TouchButton>

          {filteredAndSortedGames.length > 0 && (
            <TouchButton
              variant="outline"
              size="sm"
              leftIcon={<Share2 className="w-3.5 h-3.5 text-[#36A9E1]" />}
              onClick={() => handleShareOrExportPgn()}
            >
              Экспорт всех
            </TouchButton>
          )}
        </div>
      </div>

      {statusBanner && (
        <div className="px-4 py-2.5 rounded-xl bg-[#779954]/15 border border-[#779954] text-xs font-medium text-[#779954] flex items-center justify-between shrink-0">
          <span>{statusBanner}</span>
          <button
            type="button"
            onClick={() => setStatusBanner(null)}
            className="text-xs underline ml-2"
          >
            Скрыть
          </button>
        </div>
      )}

      {/* Search, Filter & Sort Controls */}
      <div className="p-3 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-2.5 shrink-0">
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-2.5">
          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-[var(--chess-text-secondary)] absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setVisibleLimit(PAGE_WINDOW_SIZE);
              }}
              placeholder="Поиск по игроку, дебюту ECO (напр. C50, Сицилианская), дате..."
              className="w-full min-h-[44px] pl-10 pr-3 py-2 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] text-xs sm:text-sm text-[var(--chess-text-primary)] focus:outline-none focus:border-[#779954]"
            />
          </div>

          {/* Sort Selector */}
          <div className="flex items-center gap-2 shrink-0">
            <ArrowUpDown className="w-4 h-4 text-[var(--chess-text-secondary)] shrink-0" />
            <select
              value={sortField}
              onChange={(e) => setSortField(e.target.value as ArchiveSortField)}
              className="min-h-[44px] px-3 py-2 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] text-xs font-semibold text-[var(--chess-text-primary)]"
            >
              <option value="updatedAt">Сортировка: Недавние</option>
              <option value="accuracy">Сортировка: По точности</option>
              <option value="rating">Сортировка: По рейтингу</option>
              <option value="date">Сортировка: По дате</option>
            </select>
          </div>
        </div>

        {/* Filter Segments */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <SegmentedControl
            size="sm"
            value={analysisFilter}
            onChange={(val) => {
              setAnalysisFilter(val);
              setVisibleLimit(PAGE_WINDOW_SIZE);
            }}
            options={[
              { id: 'all', label: 'Все партии' },
              { id: 'analyzed', label: 'С анализом Stockfish' },
              { id: 'unanalyzed', label: 'Без анализа' },
            ]}
          />

          <SegmentedControl
            size="sm"
            value={platformFilter}
            onChange={(val) => {
              setPlatformFilter(val);
              setVisibleLimit(PAGE_WINDOW_SIZE);
            }}
            options={[
              { id: 'all', label: 'Все источники' },
              { id: 'Chess.com', label: 'Chess.com' },
              { id: 'PGN', label: 'PGN' },
              { id: 'Analysis', label: 'Доска' },
            ]}
          />
        </div>
      </div>

      {/* Virtualized / Windowed Archive List */}
      <div
        onScroll={(e) => {
          const el = e.currentTarget;
          if (
            el.scrollHeight - el.scrollTop - el.clientHeight < 250 &&
            visibleLimit < filteredAndSortedGames.length
          ) {
            setVisibleLimit((prev) => prev + PAGE_WINDOW_SIZE);
          }
        }}
        className="flex-1 min-h-0 overflow-y-auto rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] divide-y divide-[var(--chess-border)]"
      >
        {visibleGames.length === 0 ? (
          <div className="p-10 text-center space-y-3">
            <p className="text-xs sm:text-sm text-[var(--chess-text-secondary)]">
              По выбранным критериям партий не найдено.
            </p>
            <div className="flex items-center justify-center gap-2">
              <TouchButton
                variant="primary"
                size="sm"
                leftIcon={<PlusCircle className="w-4 h-4" />}
                onClick={() => {
                  startNewGame();
                  onNavigateTab('analyzer');
                }}
              >
                Новая партия
              </TouchButton>
              <TouchButton
                variant="secondary"
                size="sm"
                leftIcon={<Globe className="w-4 h-4" />}
                onClick={onOpenChessComModal}
              >
                Импорт с Chess.com
              </TouchButton>
            </div>
          </div>
        ) : (
          visibleGames.map((rec) => {
            const isCurrent = rec.id === currentGameId;
            return (
              <div
                key={rec.id}
                onClick={() => {
                  loadSavedGameRecord(rec);
                  logUserActivity({
                    type: 'open_game',
                    title: `Открыта партия: ${rec.metadata.whiteName} vs ${rec.metadata.blackName}`,
                    subtitle: `${rec.metadata.platform} · ${rec.openingSummary?.eco || ''}`,
                    gameId: rec.id,
                  });
                  onNavigateTab('analyzer');
                }}
                className={`px-4 py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 cursor-pointer transition-colors ${
                  isCurrent
                    ? 'bg-[#779954]/12'
                    : 'hover:bg-[var(--chess-bg-secondary)]/70'
                }`}
              >
                <div className="min-w-0 flex-1 space-y-1.5">
                  <div className="flex items-start gap-2 flex-wrap min-w-0">
                    <span className="text-xs sm:text-sm font-bold text-[var(--chess-text-primary)] leading-snug break-words min-w-0">
                      ⬜ {rec.metadata.whiteName}
                      {rec.metadata.whiteRating ? ` (${rec.metadata.whiteRating})` : ''} vs ⬛{' '}
                      {rec.metadata.blackName}
                      {rec.metadata.blackRating ? ` (${rec.metadata.blackRating})` : ''}
                    </span>
                    <span className="text-xs font-mono-tabular font-semibold text-[#779954] shrink-0">
                      ({rec.metadata.result})
                    </span>
                    {isCurrent && (
                      <span className="text-[11px] font-semibold text-[#779954] shrink-0">
                        · На доске
                      </span>
                    )}
                  </div>

                  {rec.openingSummary && (
                    <div className="flex items-start gap-1.5 text-[11px] min-w-0">
                      <span className="px-1.5 py-0.5 rounded bg-[#A88865]/15 border border-[#A88865]/30 font-mono-tabular font-bold text-[#A88865] shrink-0">
                        {rec.openingSummary.eco}
                      </span>
                      <span className="text-[var(--chess-text-primary)] font-medium leading-snug break-words min-w-0 flex-1">
                        {rec.openingSummary.name}
                        {rec.openingSummary.variation &&
                        rec.openingSummary.variation !== 'Основная линия'
                          ? `: ${rec.openingSummary.variation}`
                          : ''}
                      </span>
                    </div>
                  )}

                  <div className="text-[11px] text-[var(--chess-text-secondary)] font-mono-tabular flex flex-wrap items-center gap-2">
                    <span>{rec.metadata.platform}</span>
                    <span>·</span>
                    <span>{rec.metadata.date}</span>
                    {rec.statistics ? (
                      <>
                        <span>·</span>
                        <span className="text-[#779954] font-semibold inline-flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          Точность: {rec.statistics.whiteAccuracy}% /{' '}
                          {rec.statistics.blackAccuracy}%
                        </span>
                      </>
                    ) : (
                      <>
                        <span>·</span>
                        <span>Ожидает анализа</span>
                      </>
                    )}
                  </div>
                </div>

                <div
                  onClick={(e) => e.stopPropagation()}
                  className="flex items-center gap-2 shrink-0"
                >
                  <TouchButton
                    variant="primary"
                    size="sm"
                    leftIcon={<FolderOpen className="w-3.5 h-3.5" />}
                    onClick={() => {
                      loadSavedGameRecord(rec);
                      onNavigateTab('analyzer');
                    }}
                  >
                    Анализ
                  </TouchButton>

                  <TouchButton
                    variant="secondary"
                    size="sm"
                    leftIcon={<Share2 className="w-3.5 h-3.5" />}
                    onClick={() => handleShareOrExportPgn(rec)}
                    title="Экспортировать или поделиться PGN"
                  >
                    PGN
                  </TouchButton>

                  <TouchButton
                    variant="ghost"
                    size="sm"
                    onClick={() => removeSavedGame(rec.id)}
                    title="Удалить партию из архива"
                    className="text-[var(--chess-text-secondary)] hover:text-[#E05252]"
                  >
                    <Trash2 className="w-4 h-4" />
                  </TouchButton>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Multi-Game PGN Preview Bottom Sheet */}
      <BottomSheetModal
        isOpen={isPreviewSheetOpen}
        onClose={() => setIsPreviewSheetOpen(false)}
        title={`Предпросмотр импорта PGN (${previewItems.length} парт.)`}
        subtitle="Выберите корректные партии для сохранения в локальный архив SQLite"
        icon={<FileUp className="w-5 h-5" />}
        footer={
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs font-mono-tabular text-[var(--chess-text-secondary)]">
              Выбрано: {selectedPreviewIndices.size} из{' '}
              {previewItems.filter((i) => i.isValid).length}
            </span>
            <div className="flex items-center gap-2">
              <TouchButton
                variant="secondary"
                size="sm"
                onClick={() => setIsPreviewSheetOpen(false)}
              >
                Отмена
              </TouchButton>
              <TouchButton
                variant="primary"
                size="sm"
                isLoading={isImportingBatch}
                disabled={selectedPreviewIndices.size === 0}
                leftIcon={<Download className="w-4 h-4" />}
                onClick={handleConfirmBatchImport}
              >
                Импортировать ({selectedPreviewIndices.size})
              </TouchButton>
            </div>
          </div>
        }
      >
        <div className="space-y-2">
          {previewItems.map((item) => {
            const checked = selectedPreviewIndices.has(item.index);
            return (
              <div
                key={item.index}
                onClick={() => {
                  if (!item.isValid) return;
                  const next = new Set(selectedPreviewIndices);
                  if (next.has(item.index)) next.delete(item.index);
                  else next.add(item.index);
                  setSelectedPreviewIndices(next);
                }}
                className={`p-3 rounded-xl border flex items-center justify-between gap-3 cursor-pointer ${
                  !item.isValid
                    ? 'border-[#E05252]/50 bg-[#E05252]/10 opacity-75 cursor-not-allowed'
                    : checked
                      ? 'border-[#779954] bg-[#779954]/10'
                      : 'border-[var(--chess-border)] bg-[var(--chess-bg-secondary)]'
                }`}
              >
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-bold text-[var(--chess-text-primary)] leading-snug break-words">
                    #{item.index + 1} · {item.whiteName} vs {item.blackName} ({item.result})
                  </div>
                  {item.isValid ? (
                    <div className="text-[11px] font-mono-tabular text-[var(--chess-text-secondary)] leading-snug break-words mt-0.5">
                      Ходов: {item.movesCount} · Дата: {item.date || '—'} · Событие:{' '}
                      {item.event}
                    </div>
                  ) : (
                    <div className="text-[11px] text-[#E05252] mt-0.5">
                      Ошибка PGN: {item.errorReason}
                    </div>
                  )}
                </div>

                {item.isValid && (
                  <input
                    type="checkbox"
                    checked={checked}
                    readOnly
                    className="w-4 h-4 accent-[#779954] shrink-0"
                  />
                )}
              </div>
            );
          })}
        </div>
      </BottomSheetModal>
    </div>
  );
};
