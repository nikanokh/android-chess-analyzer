/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useGameStore } from './stores/useGameStore';
import { useSettingsStore } from './stores/useSettingsStore';
import { useRepertoireStore } from './stores/useRepertoireStore';
import { useStockfishEngine } from './hooks/useStockfishEngine';
import {
  resetAllStockfishInstances,
  useMobileSystemLifecycle,
} from './engine/EngineLifecycleManager';
import { ensureDatabaseMigrated, logUserActivity } from './database/sqliteSchema';
import { calculateCapturedPiecesFromFen } from './chess/capturedPieces';
import { findNodeById } from './core/chess/VariationTree';
import { GameInfoSidebar } from './components/GameInfo/GameInfoSidebar';
import { ChessBoardView } from './components/ChessBoard/ChessBoardView';
import { EvaluationBar, getTerminalScoreFromFen } from './components/EvaluationBar/EvaluationBar';
import { AnalysisPanel } from './components/AnalysisPanel/AnalysisPanel';
import { ImportModal } from './components/Modals/ImportModal';
import { ChessComModal } from './components/Modals/ChessComModal';
import { SavedGamesModal } from './components/Modals/SavedGamesModal';
import { SettingsModal } from './components/Settings/SettingsModal';
import { OpeningRepertoirePage } from './components/Repertoire/OpeningRepertoirePage';
import { HomeScreen } from './features/home/HomeScreen';
import { GamesArchiveScreen } from './features/games/GamesArchiveScreen';
import { SettingsScreen } from './features/settings/SettingsScreen';
import { BottomTabBar } from './components/Navigation/BottomTabBar';
import { MobileDrawer } from './components/Navigation/MobileDrawer';
import { CapturedPiecesStrip } from './components/ui/MobilePrimitives';
import { MainTabId } from './theme';
import { CATEGORY_ICONS, CATEGORY_META } from './utils/assets';
import {
  ArrowLeft,
  BookOpen,
  ChevronFirst,
  ChevronLast,
  ChevronLeft,
  ChevronRight,
  CornerUpLeft,
  Globe,
  Menu,
  PanelLeftOpen,
  Pause,
  Play,
  RefreshCw,
  Square,
  Trophy,
  WifiOff,
  X,
} from 'lucide-react';

export default function App() {
  const engineState = useStockfishEngine();
  const systemState = useMobileSystemLifecycle();

  const metadata = useGameStore((s) => s.metadata);
  const rootNode = useGameStore((s) => s.rootNode);
  const currentNodeId = useGameStore((s) => s.currentNodeId);
  const openingSummary = useGameStore((s) => s.openingSummary);
  const savedGames = useGameStore((s) => s.savedGames);
  const isAutoPlaying = useGameStore((s) => s.isAutoPlaying);
  const isBatchAnalyzing = useGameStore((s) => s.isBatchAnalyzing);
  const batchProgress = useGameStore((s) => s.batchProgress);
  const errorMessage = useGameStore((s) => s.errorMessage);
  const startNewGame = useGameStore((s) => s.startNewGame);
  const goToStart = useGameStore((s) => s.goToStart);
  const goToPrev = useGameStore((s) => s.goToPrev);
  const goToNext = useGameStore((s) => s.goToNext);
  const goToEnd = useGameStore((s) => s.goToEnd);
  const returnToMainline = useGameStore((s) => s.returnToMainline);
  const setAutoPlaying = useGameStore((s) => s.setAutoPlaying);
  const runFullGameAnalysis = useGameStore((s) => s.runFullGameAnalysis);
  const cancelFullGameAnalysis = useGameStore((s) => s.cancelFullGameAnalysis);
  const clearError = useGameStore((s) => s.clearError);
  const persistCurrentGame = useGameStore((s) => s.persistCurrentGame);
  const getExportPgn = useGameStore((s) => s.getExportPgn);

  const settings = useSettingsStore((s) => s.settings);
  const updateSettings = useSettingsStore((s) => s.updateSettings);
  const flipBoard = useSettingsStore((s) => s.flipBoard);
  const toggleTheme = useSettingsStore((s) => s.toggleTheme);
  const hydrateFromStorage = useSettingsStore((s) => s.hydrateFromStorage);

  const hydrateRepertoireStore = useRepertoireStore((s) => s.hydrateRepertoireStore);
  const repertoireTaskStatus = useRepertoireStore((s) => s.engineTaskStatus);

  // Main Bottom Tab Navigation state ('analyzer' is default so chessboard is immediately visible, with 'home', 'games', 'repertoire', 'settings' in bottom tabs)
  const [activeTab, setActiveTab] = useState<MainTabId>('analyzer');
  const tabHistoryRef = useRef<MainTabId[]>(['analyzer']);

  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isChessComModalOpen, setIsChessComModalOpen] = useState(false);
  const [isSavedGamesModalOpen, setIsSavedGamesModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);

  const handleSelectTab = useCallback(
    (nextTab: MainTabId) => {
      if (nextTab === activeTab) return;
      // Save current game state whenever switching screens
      persistCurrentGame().catch(() => {});
      tabHistoryRef.current.push(nextTab);
      try {
        window.history.pushState({ tab: nextTab }, '');
      } catch {
        // ignore sandbox history limits
      }
      setActiveTab(nextTab);
    },
    [activeTab, persistCurrentGame]
  );

  // Hydrate persisted settings, SQLite/IndexedDB migration, and repertoire store on mount
  useEffect(() => {
    hydrateFromStorage();
    ensureDatabaseMigrated().catch(() => {});
    persistCurrentGame().catch(() => {});
    hydrateRepertoireStore().catch(() => {});
  }, [hydrateFromStorage, persistCurrentGame, hydrateRepertoireStore]);

  // Android System Back Button (popstate) support
  useEffect(() => {
    const handlePopState = () => {
      if (isDrawerOpen) {
        setIsDrawerOpen(false);
        return;
      }
      if (isImportModalOpen) {
        setIsImportModalOpen(false);
        return;
      }
      if (isChessComModalOpen) {
        setIsChessComModalOpen(false);
        return;
      }
      if (isSavedGamesModalOpen) {
        setIsSavedGamesModalOpen(false);
        return;
      }
      if (isSettingsModalOpen) {
        setIsSettingsModalOpen(false);
        return;
      }
      if (tabHistoryRef.current.length > 1) {
        tabHistoryRef.current.pop();
        const prevTab =
          tabHistoryRef.current[tabHistoryRef.current.length - 1] || 'analyzer';
        setActiveTab(prevTab);
      }
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [
    isDrawerOpen,
    isImportModalOpen,
    isChessComModalOpen,
    isSavedGamesModalOpen,
    isSettingsModalOpen,
  ]);

  // Keyboard navigation for moves (ArrowLeft, ArrowRight, Home/ArrowUp, End/ArrowDown, f = flip)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (activeTab !== 'analyzer') return;
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;

      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        goToPrev();
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        goToNext();
      } else if (e.key === 'ArrowUp' || e.key === 'Home') {
        e.preventDefault();
        goToStart();
      } else if (e.key === 'ArrowDown' || e.key === 'End') {
        e.preventDefault();
        goToEnd();
      } else if (e.key === 'f' || e.key === 'F') {
        e.preventDefault();
        flipBoard();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeTab, goToPrev, goToNext, goToStart, goToEnd, flipBoard]);

  // Autoplay interval
  useEffect(() => {
    if (!isAutoPlaying) return;
    const timer = window.setInterval(() => {
      goToNext();
    }, 950);
    return () => window.clearInterval(timer);
  }, [isAutoPlaying, goToNext]);

  const currentNode = findNodeById(rootNode, currentNodeId) || rootNode;
  const activeEngineLines =
    !isBatchAnalyzing && (!engineState.fen || engineState.fen === currentNode.fen)
      ? engineState.lines
      : [];
  const primaryEngineLine = activeEngineLines[0];

  // Use analyzed evaluation if available, otherwise live Stockfish evaluation
  const activeCp = currentNode.analysis?.evalAfterCp ?? primaryEngineLine?.cp ?? 0;
  const activeMate = currentNode.analysis?.mateAfter ?? primaryEngineLine?.mate;
  const terminalScore = getTerminalScoreFromFen(currentNode.fen);

  // Captured pieces & material balance for current board position
  const capturedSummary = useMemo(
    () => calculateCapturedPiecesFromFen(currentNode.fen),
    [currentNode.fen]
  );

  const isWhiteBottom = settings.boardOrientation === 'white';
  const topPlayer = isWhiteBottom
    ? {
        name: metadata.blackName,
        title: metadata.blackTitle,
        rating: metadata.blackRating,
        ratingDiff: metadata.blackRatingDiff,
        color: 'b' as const,
        capturedEnemyColor: 'w' as const,
        capturedPieces: capturedSummary.capturedWhitePieces,
        materialAdvantage: Math.max(0, -capturedSummary.materialDiffWhitePov),
      }
    : {
        name: metadata.whiteName,
        title: metadata.whiteTitle,
        rating: metadata.whiteRating,
        ratingDiff: metadata.whiteRatingDiff,
        color: 'w' as const,
        capturedEnemyColor: 'b' as const,
        capturedPieces: capturedSummary.capturedBlackPieces,
        materialAdvantage: Math.max(0, capturedSummary.materialDiffWhitePov),
      };

  const bottomPlayer = isWhiteBottom
    ? {
        name: metadata.whiteName,
        title: metadata.whiteTitle,
        rating: metadata.whiteRating,
        ratingDiff: metadata.whiteRatingDiff,
        color: 'w' as const,
        capturedEnemyColor: 'b' as const,
        capturedPieces: capturedSummary.capturedBlackPieces,
        materialAdvantage: Math.max(0, capturedSummary.materialDiffWhitePov),
      }
    : {
        name: metadata.blackName,
        title: metadata.blackTitle,
        rating: metadata.blackRating,
        ratingDiff: metadata.blackRatingDiff,
        color: 'b' as const,
        capturedEnemyColor: 'w' as const,
        capturedPieces: capturedSummary.capturedWhitePieces,
        materialAdvantage: Math.max(0, -capturedSummary.materialDiffWhitePov),
      };

  const renderRatingBadge = (rating?: number, ratingDiff?: number) => {
    if (!rating) return null;
    const sign = ratingDiff && ratingDiff > 0 ? '+' : '';
    const diffColor =
      ratingDiff && ratingDiff > 0
        ? 'text-[#779954]'
        : ratingDiff && ratingDiff < 0
          ? 'text-[#E05252]'
          : 'text-[var(--chess-text-secondary)]';

    return (
      <span className="text-[11px] font-mono-tabular text-[var(--chess-text-secondary)] shrink-0">
        {rating}
        {ratingDiff !== undefined && (
          <span className={`ml-1 font-semibold ${diffColor}`}>
            ({sign}
            {ratingDiff})
          </span>
        )}
      </span>
    );
  };

  const handleExportCurrentPgn = async () => {
    const pgn = getExportPgn();
    const filename = `${metadata.whiteName}_vs_${metadata.blackName}.pgn`.replace(/\s+/g, '_');
    logUserActivity({
      type: 'export_pgn',
      title: `Экспорт PGN: ${metadata.whiteName} vs ${metadata.blackName}`,
      subtitle: filename,
    });
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        await navigator.share({
          title: filename,
          text: pgn,
        });
        return;
      } catch {
        // fallback to download
      }
    }
    const blob = new Blob([pgn], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="h-[100dvh] w-full max-w-full overflow-hidden flex flex-col bg-[var(--chess-bg-secondary)] text-[var(--chess-text-primary)]">
      {/* Top Bar Contract: Zone 1 (Burger + Brand) — Zone 2 (Clean Nav Links) — Zone 3 (1 Primary CTA) */}
      <header className="h-12 px-3 sm:px-5 bg-[var(--chess-bg-primary)] border-b border-[var(--chess-border)] flex items-center justify-between gap-3 sm:gap-6 shrink-0 z-30 w-full max-w-full overflow-hidden">
        {/* Zone 1: Burger Drawer Trigger + Back Button + Brand Wordmark */}
        <div className="flex items-center gap-1.5 sm:gap-2 min-w-0 shrink-0">
          <button
            type="button"
            onClick={() => setIsDrawerOpen(true)}
            aria-label="Открыть боковое меню"
            className="min-w-[38px] min-h-[38px] rounded-xl flex items-center justify-center text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] active:scale-95 transition-all shrink-0"
          >
            <Menu className="w-5 h-5" />
          </button>

          {activeTab !== 'analyzer' && (
            <button
              type="button"
              onClick={() => handleSelectTab('analyzer')}
              aria-label="Назад к доске"
              className="min-w-[36px] min-h-[36px] rounded-xl flex items-center justify-center text-[var(--chess-text-secondary)] hover:text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] transition-all shrink-0"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
          )}

          {!settings.showLeftSidebar && activeTab === 'analyzer' && (
            <button
              type="button"
              onClick={() => updateSettings({ showLeftSidebar: true })}
              className="hidden xl:inline-flex p-2 rounded-lg bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)] text-[var(--chess-text-primary)] transition-colors shrink-0"
              title="Показать панель информации о партии"
            >
              <PanelLeftOpen className="w-4 h-4" />
            </button>
          )}

          <button
            type="button"
            onClick={() => handleSelectTab('home')}
            className="text-sm sm:text-base font-bold tracking-tight text-[var(--chess-text-primary)] truncate hover:opacity-90 transition-opacity"
          >
            Chess Insight
          </button>
        </div>

        {/* Zone 2: Clean Single-Line Navigation Links */}
        <nav className="hidden md:flex items-center gap-5 lg:gap-6 text-xs sm:text-sm font-medium text-[var(--chess-text-secondary)] shrink-0">
          {[
            { id: 'home' as const, label: 'Главная' },
            { id: 'games' as const, label: 'Партии' },
            { id: 'analyzer' as const, label: 'Анализ' },
            { id: 'repertoire' as const, label: 'Репертуар' },
            { id: 'settings' as const, label: 'Настройки' },
          ].map((navItem) => (
            <button
              key={navItem.id}
              type="button"
              onClick={() => handleSelectTab(navItem.id)}
              className={`py-1 transition-colors whitespace-nowrap shrink-0 border-b-2 ${
                activeTab === navItem.id
                  ? 'text-[var(--chess-text-primary)] font-bold border-[#779954]'
                  : 'border-transparent hover:text-[var(--chess-text-primary)]'
              }`}
            >
              {navItem.label}
            </button>
          ))}
        </nav>

        {/* Zone 3: 1 Primary Action CTA */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setIsChessComModalOpen(true)}
            className="min-h-[36px] px-3 py-1.5 rounded-lg text-xs font-semibold bg-[#779954] hover:bg-[#62934B] active:scale-95 text-white transition-all flex items-center gap-1.5 whitespace-nowrap shrink-0 shadow-xs"
          >
            <Globe className="w-3.5 h-3.5 shrink-0" />
            <span>Chess.com</span>
          </button>
        </div>
      </header>

      {/* Offline Banner */}
      {!systemState.isOnline && (
        <div className="bg-[#F39C36]/20 border-b border-[#F39C36] text-[#F39C36] px-3 py-1.5 text-xs font-medium flex items-center justify-center gap-2 shrink-0 w-full max-w-full">
          <WifiOff className="w-3.5 h-3.5 shrink-0" />
          <span className="break-words">
            Офлайн-режим: локальный архив SQLite, книга ECO и движок Stockfish работают без интернета.
          </span>
        </div>
      )}

      {/* Optional Error Toast Banner */}
      {errorMessage && (
        <div className="bg-[#E05252] text-white px-3 py-1.5 text-xs font-medium flex items-center justify-between gap-2 shrink-0 w-full max-w-full">
          <span className="break-words min-w-0 flex-1">{errorMessage}</span>
          <button
            type="button"
            onClick={clearError}
            className="min-w-[32px] min-h-[32px] flex items-center justify-center hover:opacity-80 shrink-0"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Scrollable Middle Workspace Container (keeps Header at top and BottomTabBar pinned at bottom) */}
      <div className="flex-1 min-h-0 w-full max-w-full overflow-y-auto md:overflow-hidden overflow-x-hidden flex flex-col">
        {activeTab === 'home' && (
          <HomeScreen
            onNavigateTab={handleSelectTab}
            onOpenImportModal={() => setIsImportModalOpen(true)}
            onOpenChessComModal={() => setIsChessComModalOpen(true)}
          />
        )}

        {activeTab === 'games' && (
          <GamesArchiveScreen
            onNavigateTab={handleSelectTab}
            onOpenChessComModal={() => setIsChessComModalOpen(true)}
            onOpenImportModal={() => setIsImportModalOpen(true)}
          />
        )}

        {activeTab === 'repertoire' && (
          <OpeningRepertoirePage
            onNavigateToAnalyzer={() => handleSelectTab('analyzer')}
          />
        )}

        {activeTab === 'settings' && <SettingsScreen />}

        {activeTab === 'analyzer' && (
          <main className="flex-1 min-h-0 max-w-[1680px] w-full mx-auto p-1.5 sm:p-2 flex flex-col md:flex-row items-stretch gap-2 overflow-visible md:overflow-hidden">
            {/* Left Zone (XL Desktop): Game Information & Full Review Trigger */}
            {settings.showLeftSidebar && <GameInfoSidebar />}

            {/* Center Zone: Chessboard + Evaluation Bar + Opening Bar + Player Bars + Move Controls */}
            <section className="shrink-0 md:flex-1 min-h-0 min-w-0 max-w-full md:h-full flex flex-col items-center justify-between bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] rounded-md px-2 sm:px-3 py-1.5 sm:py-2 overflow-hidden">
              {/* Full-Width Matchup & Opening Header Bar so long game titles and opening names always fit */}
              <div className="w-full max-w-[660px] mb-1 px-2.5 py-1.5 rounded-lg bg-[var(--chess-bg-secondary)]/85 border border-[var(--chess-border)]/70 flex flex-col gap-1 text-[11px] sm:text-xs min-w-0 shrink-0">
                <div className="flex items-start justify-between gap-2 min-w-0">
                  <span className="font-bold text-[var(--chess-text-primary)] leading-snug break-words min-w-0 flex-1">
                    {metadata.whiteName} vs {metadata.blackName}{' '}
                    <span className="font-mono-tabular font-semibold text-[#779954]">
                      ({metadata.result})
                    </span>
                  </span>
                  <span className="text-[10px] font-mono-tabular text-[var(--chess-text-secondary)] shrink-0">
                    {metadata.date}
                  </span>
                </div>

                <div className="flex items-start gap-1.5 min-w-0">
                  <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-[#A88865]/15 border border-[#A88865]/30 font-mono-tabular font-bold text-[10px] sm:text-[11px] text-[#A88865] shrink-0">
                    <BookOpen className="w-3 h-3 shrink-0" />
                    {openingSummary.eco}
                  </span>
                  <span className="text-[var(--chess-text-primary)] font-medium leading-snug break-words min-w-0 flex-1">
                    {openingSummary.name}
                    {openingSummary.variation &&
                    openingSummary.variation !== 'Основная линия' &&
                    openingSummary.variation !== 'Начальная позиция'
                      ? `: ${openingSummary.variation}`
                      : ''}
                  </span>
                </div>
              </div>

              {/* Top Player Bar + Captured Pieces */}
              <div className="w-full max-w-[660px] flex items-center justify-between mb-1 px-1 shrink-0 gap-2 min-w-0 flex-wrap">
                <div className="flex items-center gap-1.5 min-w-0 flex-1 flex-wrap">
                  <span
                    className={`w-3.5 h-3.5 rounded-xs shrink-0 border ${
                      topPlayer.color === 'w'
                        ? 'bg-[#FFFFFF] border-[#333333]'
                        : 'bg-[#262421] border-[#777777]'
                    }`}
                  />
                  {topPlayer.title && (
                    <span className="px-1.5 py-0.5 rounded-xs bg-[#B33430] text-white text-[10px] font-extrabold uppercase tracking-wider leading-none shrink-0">
                      {topPlayer.title}
                    </span>
                  )}
                  <span className="text-xs sm:text-sm font-bold text-[var(--chess-text-primary)] break-words leading-snug min-w-0">
                    {topPlayer.name}
                  </span>
                  {renderRatingBadge(topPlayer.rating, topPlayer.ratingDiff)}
                </div>

                <div className="shrink-0">
                  <CapturedPiecesStrip
                    capturedPieceColor={topPlayer.capturedEnemyColor}
                    pieces={topPlayer.capturedPieces}
                    materialAdvantage={topPlayer.materialAdvantage}
                  />
                </div>
              </div>

              {/* Board + Vertical Evaluation Bar Row */}
              <div className="md:flex-1 min-h-0 w-full max-w-full flex items-stretch justify-center gap-1.5 sm:gap-2 py-0.5">
                <EvaluationBar
                  cp={activeCp}
                  mate={activeMate}
                  fen={currentNode.fen}
                  orientation={settings.boardOrientation}
                  visible={settings.showEvalBar}
                />
                <div className="flex items-center justify-center max-w-full min-w-0">
                  <ChessBoardView engineLines={activeEngineLines} />
                </div>
              </div>

              {/* Bottom Player Bar + Captured Pieces + Current Move Classification or Terminal Result */}
              <div className="w-full max-w-[660px] flex items-center justify-between mt-1 px-1 shrink-0 gap-2 min-w-0 flex-wrap">
                <div className="flex items-center gap-1.5 min-w-0 flex-1 flex-wrap">
                  <span
                    className={`w-3.5 h-3.5 rounded-xs shrink-0 border ${
                      bottomPlayer.color === 'w'
                        ? 'bg-[#FFFFFF] border-[#333333]'
                        : 'bg-[#262421] border-[#777777]'
                    }`}
                  />
                  {bottomPlayer.title && (
                    <span className="px-1.5 py-0.5 rounded-xs bg-[#B33430] text-white text-[10px] font-extrabold uppercase tracking-wider leading-none shrink-0">
                      {bottomPlayer.title}
                    </span>
                  )}
                  <span className="text-xs sm:text-sm font-bold text-[var(--chess-text-primary)] break-words leading-snug min-w-0">
                    {bottomPlayer.name}
                  </span>
                  {renderRatingBadge(bottomPlayer.rating, bottomPlayer.ratingDiff)}
                  <CapturedPiecesStrip
                    capturedPieceColor={bottomPlayer.capturedEnemyColor}
                    pieces={bottomPlayer.capturedPieces}
                    materialAdvantage={bottomPlayer.materialAdvantage}
                  />
                </div>

                <div className="flex items-center gap-2 shrink-0 min-w-0 flex-wrap">
                  {terminalScore && (
                    <span className="px-2 py-0.5 rounded bg-[#779954]/20 text-[#779954] font-mono-tabular font-bold text-[11px] flex items-center gap-1 shrink-0">
                      <Trophy className="w-3 h-3" />
                      {terminalScore === '1/2-1/2'
                        ? `Ничья · ${terminalScore}`
                        : `Мат · ${terminalScore}`}
                    </span>
                  )}

                  {currentNode.analysis && (
                    <div className="flex items-center gap-1 text-[11px] sm:text-xs min-w-0">
                      <img
                        src={CATEGORY_ICONS[currentNode.analysis.category]}
                        alt={currentNode.analysis.category}
                        referrerPolicy="no-referrer"
                        className="w-3.5 h-3.5 sm:w-4 sm:h-4 object-contain shrink-0"
                      />
                      <span
                        style={{ color: CATEGORY_META[currentNode.analysis.category].color }}
                        className="font-semibold break-words leading-snug min-w-0"
                      >
                        {currentNode.moveNumber}
                        {currentNode.color === 'w' ? '.' : '...'} {currentNode.san} —{' '}
                        {CATEGORY_META[currentNode.analysis.category].labelRu}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Game Navigation Control Bar Below Board (Clean & Compact Desktop Layout) */}
              <div className="w-full max-w-[660px] mt-1.5 pt-1.5 border-t border-[var(--chess-border)] flex items-center justify-between gap-1 shrink-0 flex-wrap">
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={goToStart}
                    disabled={currentNode.id === 'root'}
                    className="p-1.5 rounded bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)] disabled:opacity-40 text-[var(--chess-text-primary)] transition-colors"
                    title="В начало партии (Home / Стрелка вверх)"
                  >
                    <ChevronFirst className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={goToPrev}
                    disabled={!currentNode.parentId}
                    className="p-1.5 rounded bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)] disabled:opacity-40 text-[var(--chess-text-primary)] transition-colors"
                    title="Предыдущий ход (Стрелка влево)"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={() => setAutoPlaying(!isAutoPlaying)}
                    className={`px-2.5 py-1.5 rounded font-semibold text-xs transition-colors flex items-center gap-1 ${
                      isAutoPlaying
                        ? 'bg-[#F39C36] text-white'
                        : 'bg-[#779954] hover:bg-[#62934B] text-white'
                    }`}
                    title="Автовоспроизведение партии"
                  >
                    {isAutoPlaying ? (
                      <>
                        <Pause className="w-3.5 h-3.5 fill-current" />
                        <span>Пауза</span>
                      </>
                    ) : (
                      <>
                        <Play className="w-3.5 h-3.5 fill-current" />
                        <span>Авто</span>
                      </>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={goToNext}
                    disabled={currentNode.children.length === 0}
                    className="p-1.5 rounded bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)] disabled:opacity-40 text-[var(--chess-text-primary)] transition-colors"
                    title="Следующий ход (Стрелка вправо)"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={goToEnd}
                    disabled={currentNode.children.length === 0}
                    className="p-1.5 rounded bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)] disabled:opacity-40 text-[var(--chess-text-primary)] transition-colors"
                    title="В конец партии (End / Стрелка вниз)"
                  >
                    <ChevronLast className="w-4 h-4" />
                  </button>

                  {/* One-Click Return from Alternative Variation Branch to Mainline */}
                  {!currentNode.isMainline && (
                    <button
                      type="button"
                      onClick={returnToMainline}
                      className="ml-1 px-2 py-1.5 rounded bg-[#36A9E1] hover:bg-[#2B93C7] text-white text-xs font-semibold transition-colors flex items-center gap-1 whitespace-nowrap shadow-xs"
                      title="Вернуться из альтернативной ветки к основной партии одним нажатием"
                    >
                      <CornerUpLeft className="w-3.5 h-3.5" />
                      <span>К основной</span>
                    </button>
                  )}
                </div>

                <div className="flex items-center gap-1.5">
                  {/* Quick Full Analysis Button when Left Sidebar is hidden (< xl) */}
                  {isBatchAnalyzing ? (
                    <button
                      type="button"
                      onClick={cancelFullGameAnalysis}
                      className="xl:hidden px-2.5 py-1.5 rounded bg-[#E05252] text-white text-xs font-semibold flex items-center gap-1 whitespace-nowrap"
                    >
                      <Square className="w-3 h-3" />
                      <span className="font-mono-tabular">
                        Стоп ({batchProgress.current}/{batchProgress.total})
                      </span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => {
                        logUserActivity({
                          type: 'analyze_game',
                          title: `Полный анализ Stockfish: ${metadata.whiteName} vs ${metadata.blackName}`,
                          subtitle: `Глубина ${settings.engineConfig.depth}`,
                        });
                        runFullGameAnalysis(settings.engineConfig.depth, settings.thresholds);
                      }}
                      className="xl:hidden px-2.5 py-1.5 rounded bg-[#779954] hover:bg-[#62934B] text-white text-xs font-semibold flex items-center gap-1 whitespace-nowrap transition-colors"
                      title="Запустить полный анализ партии"
                    >
                      <Play className="w-3 h-3 fill-current" />
                      <span>Анализ</span>
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={flipBoard}
                    className="px-2.5 py-1.5 rounded bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)] text-xs font-medium text-[var(--chess-text-primary)] transition-colors flex items-center gap-1 whitespace-nowrap"
                    title="Перевернуть доску (клавиша F)"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    <span>Переворот</span>
                  </button>
                </div>
              </div>
            </section>

            {/* Right / Bottom Zone: Analysis Panel (Ходы | Анализ | Статистика | Партия) */}
            <AnalysisPanel engineState={engineState} />
          </main>
        )}
      </div>

      {/* Bottom Tab Navigation Bar (Главная | Партии | Анализ | Репертуар | Настройки) */}
      <BottomTabBar
        activeTab={activeTab}
        onSelectTab={handleSelectTab}
        savedGamesCount={savedGames.length}
        isAnalyzing={isBatchAnalyzing}
        repertoireRunning={repertoireTaskStatus === 'running'}
      />

      {/* Slide-out Mobile Drawer (Burger Menu) */}
      <MobileDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        activeTab={activeTab}
        onSelectTab={handleSelectTab}
        onOpenImportModal={() => setIsImportModalOpen(true)}
        onOpenChessComModal={() => setIsChessComModalOpen(true)}
        onStartNewGame={startNewGame}
        onExportCurrentPgn={handleExportCurrentPgn}
        onResetEngine={resetAllStockfishInstances}
        theme={settings.theme}
        onToggleTheme={toggleTheme}
        engineState={engineState}
        isOnline={systemState.isOnline}
        savedGamesCount={savedGames.length}
      />

      {/* Modals */}
      <ImportModal isOpen={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
      <ChessComModal isOpen={isChessComModalOpen} onClose={() => setIsChessComModalOpen(false)} />
      <SavedGamesModal
        isOpen={isSavedGamesModalOpen}
        onClose={() => setIsSavedGamesModalOpen(false)}
      />
      <SettingsModal isOpen={isSettingsModalOpen} onClose={() => setIsSettingsModalOpen(false)} />
    </div>
  );
}
