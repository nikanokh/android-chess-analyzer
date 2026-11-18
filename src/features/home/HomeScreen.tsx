import React, { useMemo } from 'react';
import { useGameStore } from '../../stores/useGameStore';
import { useRepertoireStore } from '../../stores/useRepertoireStore';
import { useSettingsStore } from '../../stores/useSettingsStore';
import { MainTabId } from '../../theme';
import { TouchButton } from '../../components/ui/MobilePrimitives';
import { getRecentActivities, logUserActivity } from '../../database/sqliteSchema';
import { SAMPLE_MASTER_GAMES } from '../../services/PgnService/pgnService';
import {
  Activity,
  ArrowRight,
  BookOpen,
  ChevronRight,
  Clock,
  Cpu,
  Database,
  FileUp,
  Globe,
  Play,
  PlusCircle,
  Sparkles,
  Trophy,
} from 'lucide-react';

interface HomeScreenProps {
  onNavigateTab: (tab: MainTabId) => void;
  onOpenImportModal: () => void;
  onOpenChessComModal: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  onNavigateTab,
  onOpenImportModal,
  onOpenChessComModal,
}) => {
  const metadata = useGameStore((s) => s.metadata);
  const openingSummary = useGameStore((s) => s.openingSummary);
  const statistics = useGameStore((s) => s.statistics);
  const savedGames = useGameStore((s) => s.savedGames);
  const isBatchAnalyzing = useGameStore((s) => s.isBatchAnalyzing);
  const batchProgress = useGameStore((s) => s.batchProgress);
  const loadSavedGameRecord = useGameStore((s) => s.loadSavedGameRecord);
  const loadPgnGame = useGameStore((s) => s.loadPgnGame);
  const startNewGame = useGameStore((s) => s.startNewGame);
  const runFullGameAnalysis = useGameStore((s) => s.runFullGameAnalysis);

  const settings = useSettingsStore((s) => s.settings);
  const cacheStats = useRepertoireStore((s) => s.cacheStats);
  const aggregatedRepertoire = useRepertoireStore((s) => s.aggregated);

  // Compute overall statistics across saved games
  const overallMetrics = useMemo(() => {
    const totalGames = savedGames.length;
    const analyzedGames = savedGames.filter((g) => Boolean(g.statistics));
    let avgAccSum = 0;
    for (const g of analyzedGames) {
      if (g.statistics) {
        avgAccSum += (g.statistics.whiteAccuracy + g.statistics.blackAccuracy) / 2;
      }
    }
    const meanAccuracy =
      analyzedGames.length > 0 ? Math.round((avgAccSum / analyzedGames.length) * 10) / 10 : null;
    const uniqueOpenings = new Set(
      savedGames.map((g) => g.openingSummary?.eco || g.metadata.eco).filter(Boolean)
    ).size;

    return {
      totalGames,
      analyzedCount: analyzedGames.length,
      meanAccuracy,
      uniqueOpenings: Math.max(
        uniqueOpenings,
        aggregatedRepertoire?.openings?.length || 0
      ),
    };
  }, [savedGames, aggregatedRepertoire]);

  const recentGames = useMemo(() => savedGames.slice(0, 5), [savedGames]);
  const recentActivities = useMemo(() => getRecentActivities().slice(0, 5), [savedGames.length]);

  const formatTimeAgo = (ts: number) => {
    const diffMin = Math.max(1, Math.round((Date.now() - ts) / 60000));
    if (diffMin < 60) return `${diffMin} мин назад`;
    const diffHours = Math.round(diffMin / 60);
    if (diffHours < 24) return `${diffHours} ч назад`;
    return new Date(ts).toLocaleDateString('ru-RU');
  };

  return (
    <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden p-3 sm:p-5 space-y-5 max-w-6xl w-full mx-auto">
      {/* 1. Active Game Hero Banner & Quick Analysis Resume */}
      <section className="p-4 sm:p-5 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] shadow-sm flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
        <div className="space-y-2 min-w-0 flex-1 w-full">
          <div className="flex flex-wrap items-center gap-2 text-xs text-[var(--chess-text-secondary)] font-mono-tabular">
            <span className="text-[#779954] font-bold">Текущая партия на доске</span>
            <span>·</span>
            <span>{metadata.platform}</span>
            <span>·</span>
            <span>{metadata.date}</span>
          </div>

          <h1 className="text-base sm:text-xl font-bold text-[var(--chess-text-primary)] leading-snug break-words min-w-0">
            {metadata.whiteName} vs {metadata.blackName}{' '}
            <span className="font-mono-tabular text-sm font-normal text-[var(--chess-text-secondary)]">
              ({metadata.result})
            </span>
          </h1>

          <div className="flex items-start gap-2 text-xs text-[var(--chess-text-secondary)] min-w-0">
            <span className="px-1.5 py-0.5 rounded bg-[#A88865]/15 border border-[#A88865]/30 font-mono-tabular font-bold text-[#A88865] shrink-0">
              {openingSummary.eco}
            </span>
            <div className="min-w-0 flex-1 leading-snug break-words">
              <span className="text-[var(--chess-text-primary)] font-medium">
                {openingSummary.name}
                {openingSummary.variation ? `: ${openingSummary.variation}` : ''}
              </span>
              {statistics && (
                <span className="ml-2 font-mono-tabular font-semibold text-[#779954] inline-block">
                  · Точность: {statistics.whiteAccuracy}% / {statistics.blackAccuracy}%
                </span>
              )}
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 w-full lg:w-auto shrink-0">
          <TouchButton
            variant="primary"
            size="md"
            leftIcon={<ArrowRight className="w-4 h-4" />}
            onClick={() => onNavigateTab('analyzer')}
            className="flex-1 sm:flex-initial"
          >
            Перейти к доске
          </TouchButton>

          <TouchButton
            variant="secondary"
            size="md"
            isLoading={isBatchAnalyzing}
            leftIcon={<Play className="w-4 h-4 text-[#779954] fill-current" />}
            onClick={() => {
              onNavigateTab('analyzer');
              if (!isBatchAnalyzing) {
                logUserActivity({
                  type: 'analyze_game',
                  title: `Запущен анализ Stockfish: ${metadata.whiteName} vs ${metadata.blackName}`,
                  subtitle: `Глубина ${settings.engineConfig.depth} · ${openingSummary.eco}`,
                });
                runFullGameAnalysis(settings.engineConfig.depth, settings.thresholds);
              }
            }}
            className="flex-1 sm:flex-initial"
          >
            {isBatchAnalyzing
              ? `Анализ ${batchProgress.current}/${batchProgress.total}`
              : statistics
                ? 'Пересчитать'
                : 'Запустить анализ'}
          </TouchButton>
        </div>
      </section>

      {/* 2. Key Performance & Storage KPI Strip (4 cards) */}
      <section className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div
          onClick={() => onNavigateTab('games')}
          className="p-3.5 sm:p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] hover:border-[#779954] cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between text-xs text-[var(--chess-text-secondary)]">
            <span>Партий в архиве</span>
            <Database className="w-4 h-4 text-[#779954]" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold font-mono-tabular text-[var(--chess-text-primary)] mt-1.5">
            {overallMetrics.totalGames}
          </div>
          <div className="text-[11px] text-[var(--chess-text-secondary)] mt-1">
            Проанализировано: {overallMetrics.analyzedCount}
          </div>
        </div>

        <div
          onClick={() => onNavigateTab('analyzer')}
          className="p-3.5 sm:p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] hover:border-[#779954] cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between text-xs text-[var(--chess-text-secondary)]">
            <span>Средняя точность</span>
            <Trophy className="w-4 h-4 text-[#F39C36]" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold font-mono-tabular text-[#779954] mt-1.5">
            {overallMetrics.meanAccuracy !== null ? `${overallMetrics.meanAccuracy}%` : '—'}
          </div>
          <div className="text-[11px] text-[var(--chess-text-secondary)] mt-1">
            {overallMetrics.analyzedCount > 0
              ? `По ${overallMetrics.analyzedCount} партиям Stockfish`
              : 'Запустите анализ партии'}
          </div>
        </div>

        <div
          onClick={() => onNavigateTab('repertoire')}
          className="p-3.5 sm:p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] hover:border-[#779954] cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between text-xs text-[var(--chess-text-secondary)]">
            <span>Дебютный репертуар</span>
            <BookOpen className="w-4 h-4 text-[#A88865]" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold font-mono-tabular text-[var(--chess-text-primary)] mt-1.5">
            {overallMetrics.uniqueOpenings}
          </div>
          <div className="text-[11px] text-[var(--chess-text-secondary)] mt-1">
            В кэше репертуара: {cacheStats.totalCachedGames} парт.
          </div>
        </div>

        <div
          onClick={() => onNavigateTab('settings')}
          className="p-3.5 sm:p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] hover:border-[#779954] cursor-pointer transition-colors"
        >
          <div className="flex items-center justify-between text-xs text-[var(--chess-text-secondary)]">
            <span>Движок Stockfish UCI</span>
            <Cpu className="w-4 h-4 text-[#36A9E1]" />
          </div>
          <div className="text-2xl sm:text-3xl font-bold font-mono-tabular text-[var(--chess-text-primary)] mt-1.5">
            d{settings.engineConfig.depth}
          </div>
          <div className="text-[11px] text-[var(--chess-text-secondary)] mt-1">
            MultiPV {settings.engineConfig.multiPv} · Hash {settings.engineConfig.hashMb} МБ
          </div>
        </div>
      </section>

      {/* 3. Quick Actions Row: Import PGN, Chess.com Profile, New Game, Repertoire */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <TouchButton
          variant="primary"
          size="lg"
          fullWidth
          leftIcon={<Globe className="w-4 h-4" />}
          onClick={onOpenChessComModal}
        >
          Профиль Chess.com
        </TouchButton>

        <TouchButton
          variant="secondary"
          size="lg"
          fullWidth
          leftIcon={<FileUp className="w-4 h-4 text-[#F39C36]" />}
          onClick={onOpenImportModal}
        >
          Быстрый импорт PGN / FEN
        </TouchButton>

        <TouchButton
          variant="secondary"
          size="lg"
          fullWidth
          leftIcon={<PlusCircle className="w-4 h-4 text-[#779954]" />}
          onClick={() => {
            startNewGame();
            logUserActivity({
              type: 'open_game',
              title: 'Создана новая партия на доске',
              subtitle: 'Свободный анализ с начальной позиции',
            });
            onNavigateTab('analyzer');
          }}
        >
          Новая партия на доске
        </TouchButton>

        <TouchButton
          variant="secondary"
          size="lg"
          fullWidth
          leftIcon={<BookOpen className="w-4 h-4 text-[#A88865]" />}
          onClick={() => onNavigateTab('repertoire')}
        >
          Анализ репертуара
        </TouchButton>
      </section>

      {/* 4. Two-Column Main Feed: Recent Games & Recent User Actions + Master Samples */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-stretch">
        {/* Left 7 cols: Recent Games from Archive */}
        <section className="lg:col-span-7 p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] flex flex-col justify-between space-y-3">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm sm:text-base font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
                <Database className="w-4 h-4 text-[#779954]" />
                <span>Последние партии</span>
              </h2>
              <button
                type="button"
                onClick={() => onNavigateTab('games')}
                className="min-h-[36px] px-2 text-xs font-semibold text-[#779954] hover:underline flex items-center gap-1"
              >
                <span>Весь архив ({savedGames.length})</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {recentGames.length === 0 ? (
              <div className="p-6 rounded-xl bg-[var(--chess-bg-secondary)] text-center text-xs text-[var(--chess-text-secondary)]">
                Архив пока пуст. Импортируйте партии с Chess.com или загрузите файл PGN.
              </div>
            ) : (
              <div className="divide-y divide-[var(--chess-border)] rounded-xl border border-[var(--chess-border)] overflow-hidden">
                {recentGames.map((rec) => (
                  <button
                    key={rec.id}
                    type="button"
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
                    className="w-full min-h-[60px] px-3.5 py-2.5 bg-[var(--chess-bg-secondary)]/50 hover:bg-[var(--chess-bg-secondary)] flex items-center justify-between gap-3 text-left transition-colors active:scale-[0.99]"
                  >
                    <div className="min-w-0 flex-1 space-y-1">
                      <div className="text-xs sm:text-sm font-bold text-[var(--chess-text-primary)] leading-snug break-words">
                        {rec.metadata.whiteName} vs {rec.metadata.blackName}{' '}
                        <span className="font-mono-tabular font-normal text-[var(--chess-text-secondary)]">
                          ({rec.metadata.result})
                        </span>
                      </div>
                      <div className="text-[11px] text-[var(--chess-text-secondary)] font-mono-tabular flex flex-wrap items-center gap-1.5">
                        <span>{rec.metadata.platform}</span>
                        <span>·</span>
                        <span>{rec.metadata.date}</span>
                        {rec.openingSummary && (
                          <>
                            <span className="px-1.5 py-0.2 rounded bg-[#A88865]/15 text-[#A88865] font-bold shrink-0">
                              {rec.openingSummary.eco}
                            </span>
                            <span className="break-words leading-snug min-w-0">
                              {rec.openingSummary.name}
                              {rec.openingSummary.variation &&
                              rec.openingSummary.variation !== 'Основная линия'
                                ? `: ${rec.openingSummary.variation}`
                                : ''}
                            </span>
                          </>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {rec.statistics ? (
                        <div className="text-right font-mono-tabular">
                          <div className="text-xs font-bold text-[#779954]">
                            {rec.statistics.whiteAccuracy}% / {rec.statistics.blackAccuracy}%
                          </div>
                          <div className="text-[10px] text-[var(--chess-text-secondary)]">
                            Точность
                          </div>
                        </div>
                      ) : (
                        <span className="text-[11px] text-[var(--chess-text-secondary)]">
                          Открыть
                        </span>
                      )}
                      <ChevronRight className="w-4 h-4 text-[var(--chess-text-secondary)]" />
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Master Sample Games Quick Strip */}
          <div className="pt-2 border-t border-[var(--chess-border)] space-y-2">
            <div className="text-xs font-semibold text-[var(--chess-text-secondary)] flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-[#A88865]" />
              <span>Классические партии для быстрого теста анализатора:</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              {SAMPLE_MASTER_GAMES.map((sample, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    loadPgnGame(sample.pgn, 'PGN');
                    logUserActivity({
                      type: 'import_pgn',
                      title: `Загружена классическая партия: ${sample.title}`,
                      subtitle: sample.subtitle,
                    });
                    onNavigateTab('analyzer');
                  }}
                  className="min-h-[48px] p-2.5 rounded-xl bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)]/60 border border-[var(--chess-border)] text-left transition-colors"
                >
                  <div className="text-xs font-bold text-[var(--chess-text-primary)] leading-snug break-words">
                    {sample.title}
                  </div>
                  <div className="text-[10px] text-[var(--chess-text-secondary)] leading-snug break-words mt-0.5">
                    {sample.subtitle}
                  </div>
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Right 5 cols: Recent User Activity Log */}
        <section className="lg:col-span-5 p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] flex flex-col justify-between space-y-3">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm sm:text-base font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
                <Activity className="w-4 h-4 text-[#36A9E1]" />
                <span>Последние действия</span>
              </h2>
              <span className="text-[11px] font-mono-tabular text-[var(--chess-text-secondary)]">
                Журнал БД
              </span>
            </div>

            <div className="divide-y divide-[var(--chess-border)]/70">
              {recentActivities.map((act) => (
                <div key={act.id} className="py-2.5 flex items-start justify-between gap-2.5">
                  <div className="min-w-0 flex-1">
                    <div className="text-xs font-semibold text-[var(--chess-text-primary)] leading-snug break-words">
                      {act.title}
                    </div>
                    <div className="text-[11px] text-[var(--chess-text-secondary)] leading-snug break-words mt-0.5">
                      {act.subtitle}
                    </div>
                  </div>
                  <span className="text-[10px] font-mono-tabular text-[var(--chess-text-secondary)] flex items-center gap-1 shrink-0">
                    <Clock className="w-3 h-3" />
                    {formatTimeAgo(act.timestamp)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="p-3 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] text-xs text-[var(--chess-text-secondary)] flex items-center justify-between gap-2">
            <span>Локальное хранилище SQLite v3 активно</span>
            <button
              type="button"
              onClick={() => onNavigateTab('settings')}
              className="font-semibold text-[#779954] hover:underline shrink-0"
            >
              Параметры →
            </button>
          </div>
        </section>
      </div>
    </div>
  );
};
