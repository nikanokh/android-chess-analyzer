import React, { useEffect, useState } from 'react';
import { MainTabId } from '../../theme';
import { EngineState } from '../../types/chess';
import {
  BookOpen,
  Cpu,
  Database,
  Download,
  FileUp,
  Globe,
  HardDrive,
  Home,
  Moon,
  PlusCircle,
  RefreshCw,
  Settings,
  Smartphone,
  Sun,
  Trophy,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';

interface MobileDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  activeTab: MainTabId;
  onSelectTab: (tab: MainTabId) => void;
  onOpenImportModal: () => void;
  onOpenChessComModal: () => void;
  onStartNewGame: () => void;
  onExportCurrentPgn: () => void;
  onResetEngine: () => void;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  engineState: EngineState;
  isOnline: boolean;
  savedGamesCount: number;
}

/**
 * Full-featured Android Slide-Out Drawer (Burger Menu).
 * Supports touch swipe-left to close, backdrop click close, grouped sections,
 * active section indication, and real-time Stockfish & network status.
 */
export const MobileDrawer: React.FC<MobileDrawerProps> = ({
  isOpen,
  onClose,
  activeTab,
  onSelectTab,
  onOpenImportModal,
  onOpenChessComModal,
  onStartNewGame,
  onExportCurrentPgn,
  onResetEngine,
  theme,
  onToggleTheme,
  engineState,
  isOnline,
  savedGamesCount,
}) => {
  const [touchStartX, setTouchStartX] = useState<number | null>(null);
  const [dragOffsetX, setDragOffsetX] = useState(0);
  const [isResettingEngine, setIsResettingEngine] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      setDragOffsetX(0);
      return;
    }
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleNavigate = (tab: MainTabId) => {
    onSelectTab(tab);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/65 backdrop-blur-xs flex justify-start"
      onClick={onClose}
    >
      <aside
        onClick={(e) => e.stopPropagation()}
        onTouchStart={(e) => setTouchStartX(e.touches[0].clientX)}
        onTouchMove={(e) => {
          if (touchStartX === null) return;
          const delta = e.touches[0].clientX - touchStartX;
          if (delta < 0) {
            setDragOffsetX(delta);
          }
        }}
        onTouchEnd={() => {
          if (dragOffsetX < -75) {
            onClose();
          }
          setDragOffsetX(0);
          setTouchStartX(null);
        }}
        style={{
          transform: dragOffsetX < 0 ? `translateX(${dragOffsetX}px)` : undefined,
        }}
        className="w-[300px] sm:w-[320px] max-w-[86vw] h-full bg-[var(--chess-bg-primary)] border-r border-[var(--chess-border)] shadow-2xl flex flex-col justify-between overflow-hidden transition-transform duration-150 select-none"
      >
        {/* Top Brand & Status Header */}
        <div className="p-4 bg-[var(--chess-bg-secondary)] border-b border-[var(--chess-border)] space-y-3">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-[#779954] text-white flex items-center justify-center font-bold shadow-sm shrink-0">
                ♟
              </div>
              <div className="min-w-0">
                <div className="text-sm font-bold text-[var(--chess-text-primary)] truncate">
                  Chess Insight Mobile
                </div>
                <div className="text-[11px] text-[var(--chess-text-secondary)] truncate">
                  Stockfish 16 UCI · Android & Expo
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              aria-label="Закрыть меню"
              className="min-w-[44px] min-h-[44px] rounded-xl flex items-center justify-center text-[var(--chess-text-secondary)] hover:text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-primary)] transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Engine & Connectivity Status Bar */}
          <div className="p-2.5 rounded-xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] flex items-center justify-between text-[11px] font-mono-tabular">
            <div className="flex items-center gap-1.5 text-[var(--chess-text-primary)]">
              <Cpu className="w-3.5 h-3.5 text-[#779954]" />
              <span>
                {engineState.isReady
                  ? engineState.isAnalyzing
                    ? `Анализ d${engineState.currentDepth}`
                    : `Готов (d${engineState.currentDepth})`
                  : 'Инициализация...'}
              </span>
            </div>

            <div className="flex items-center gap-1 text-[var(--chess-text-secondary)]">
              {isOnline ? (
                <>
                  <Wifi className="w-3.5 h-3.5 text-[#779954]" />
                  <span>Онлайн</span>
                </>
              ) : (
                <>
                  <WifiOff className="w-3.5 h-3.5 text-[#F39C36]" />
                  <span className="text-[#F39C36]">Офлайн</span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Scrollable Menu Groups */}
        <div className="flex-1 overflow-y-auto p-3 space-y-5 text-xs sm:text-sm">
          {/* Group 1: Quick Game Actions (not permanently in bottom bar) */}
          <div className="space-y-1">
            <div className="px-2.5 py-1 text-[11px] font-semibold text-[var(--chess-text-secondary)]">
              Действия с партиями
            </div>

            <button
              type="button"
              onClick={() => {
                onStartNewGame();
                onSelectTab('analyzer');
                onClose();
              }}
              className="w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center gap-3 text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] active:scale-[0.99] transition-all font-medium"
            >
              <PlusCircle className="w-4 h-4 text-[#779954] shrink-0" />
              <span>Новая партия на доске</span>
            </button>

            <button
              type="button"
              onClick={() => {
                onClose();
                onOpenImportModal();
              }}
              className="w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center gap-3 text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] active:scale-[0.99] transition-all font-medium"
            >
              <FileUp className="w-4 h-4 text-[#F39C36] shrink-0" />
              <span>Импорт PGN / FEN / Классика</span>
            </button>

            <button
              type="button"
              onClick={() => {
                onClose();
                onOpenChessComModal();
              }}
              className="w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center justify-between gap-3 text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] active:scale-[0.99] transition-all font-medium"
            >
              <div className="flex items-center gap-3">
                <Globe className="w-4 h-4 text-[#779954] shrink-0" />
                <span>Загрузить с Chess.com</span>
              </div>
              <span className="text-[10px] font-mono-tabular text-[var(--chess-text-secondary)]">
                PubAPI
              </span>
            </button>

            <button
              type="button"
              onClick={() => {
                onExportCurrentPgn();
                onClose();
              }}
              className="w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center gap-3 text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] active:scale-[0.99] transition-all font-medium"
            >
              <Download className="w-4 h-4 text-[#36A9E1] shrink-0" />
              <span>Экспорт и отправка PGN</span>
            </button>
          </div>

          {/* Group 2: App Sections & Tools */}
          <div className="space-y-1 border-t border-[var(--chess-border)] pt-3">
            <div className="px-2.5 py-1 text-[11px] font-semibold text-[var(--chess-text-secondary)]">
              Разделы и инструменты
            </div>

            {[
              { id: 'home' as const, label: 'Главная сводка', icon: Home },
              {
                id: 'games' as const,
                label: `Архив партий (${savedGamesCount})`,
                icon: Database,
              },
              { id: 'analyzer' as const, label: 'Шахматная доска и анализ', icon: Trophy },
              { id: 'repertoire' as const, label: 'Дебютный репертуар ECO', icon: BookOpen },
              {
                id: 'settings' as const,
                label: 'Настройки и сборка APK',
                icon: Smartphone,
              },
            ].map((sec) => {
              const Icon = sec.icon;
              const isCurrent = activeTab === sec.id;
              return (
                <button
                  key={sec.id}
                  type="button"
                  onClick={() => handleNavigate(sec.id)}
                  className={`w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center justify-between gap-3 transition-all ${
                    isCurrent
                      ? 'bg-[#779954]/15 text-[#779954] font-bold'
                      : 'text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] font-medium'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <Icon className="w-4 h-4 shrink-0" />
                    <span className="truncate">{sec.label}</span>
                  </div>
                  {isCurrent && (
                    <span className="text-[10px] font-mono-tabular text-[#779954]">
                      Активен
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          {/* Group 3: System & Engine Diagnostics */}
          <div className="space-y-1 border-t border-[var(--chess-border)] pt-3">
            <div className="px-2.5 py-1 text-[11px] font-semibold text-[var(--chess-text-secondary)]">
              Система и движок
            </div>

            <button
              type="button"
              disabled={isResettingEngine}
              onClick={async () => {
                setIsResettingEngine(true);
                try {
                  await onResetEngine();
                } finally {
                  setIsResettingEngine(false);
                }
              }}
              className="w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center gap-3 text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] transition-all font-medium disabled:opacity-50"
            >
              <RefreshCw
                className={`w-4 h-4 text-[#36A9E1] shrink-0 ${
                  isResettingEngine ? 'animate-spin' : ''
                }`}
              />
              <span>Перезапустить Stockfish UCI</span>
            </button>

            <button
              type="button"
              onClick={onToggleTheme}
              className="w-full min-h-[46px] px-3 py-2 rounded-xl flex items-center justify-between gap-3 text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] transition-all font-medium"
            >
              <div className="flex items-center gap-3">
                {theme === 'dark' ? (
                  <Sun className="w-4 h-4 text-[#F39C36] shrink-0" />
                ) : (
                  <Moon className="w-4 h-4 text-[#36A9E1] shrink-0" />
                )}
                <span>Тема оформления</span>
              </div>
              <span className="text-xs text-[var(--chess-text-secondary)]">
                {theme === 'dark' ? 'Тёмная' : 'Светлая'}
              </span>
            </button>
          </div>
        </div>

        {/* Footer Info */}
        <div className="p-3.5 border-t border-[var(--chess-border)] bg-[var(--chess-bg-secondary)] flex items-center justify-between text-[11px] text-[var(--chess-text-secondary)]">
          <span className="flex items-center gap-1.5">
            <HardDrive className="w-3.5 h-3.5 text-[#779954]" />
            <span>SQLite v3 + IndexedDB</span>
          </span>
          <span className="font-mono-tabular">SDK 53 · RN</span>
        </div>
      </aside>
    </div>
  );
};
