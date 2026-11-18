import React from 'react';
import { MainTabId } from '../../theme';
import {
  BookOpen,
  Compass,
  Cpu,
  Database,
  Home,
  Settings,
} from 'lucide-react';

interface BottomTabBarProps {
  activeTab: MainTabId;
  onSelectTab: (tab: MainTabId) => void;
  savedGamesCount: number;
  isAnalyzing: boolean;
  repertoireRunning: boolean;
}

const TAB_ITEMS: Array<{
  id: MainTabId;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}> = [
  { id: 'home', label: 'Главная', icon: Home },
  { id: 'games', label: 'Партии', icon: Database },
  { id: 'analyzer', label: 'Анализ', icon: Cpu },
  { id: 'repertoire', label: 'Репертуар', icon: BookOpen },
  { id: 'settings', label: 'Настройки', icon: Settings },
];

/**
 * Android / Expo Router style Bottom Tab Navigation Bar.
 * Enforces >= 48dp touch hitboxes, clear active state indicator, and single-line labels.
 */
export const BottomTabBar: React.FC<BottomTabBarProps> = ({
  activeTab,
  onSelectTab,
  savedGamesCount,
  isAnalyzing,
  repertoireRunning,
}) => {
  return (
    <nav
      aria-label="Основная нижняя навигация"
      className="h-14 sm:h-15 w-full max-w-full bg-[var(--chess-bg-primary)]/95 backdrop-blur-md border-t border-[var(--chess-border)] grid grid-cols-5 items-stretch px-1 sm:px-3 shrink-0 sticky bottom-0 left-0 right-0 z-40 select-none overflow-hidden"
    >
      {TAB_ITEMS.map((item) => {
        const Icon = item.icon;
        const isActive = activeTab === item.id;
        const showBadge =
          (item.id === 'games' && savedGamesCount > 0) ||
          (item.id === 'analyzer' && isAnalyzing) ||
          (item.id === 'repertoire' && repertoireRunning);

        return (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelectTab(item.id)}
            className={`relative min-h-[48px] min-w-0 flex flex-col items-center justify-center gap-0.5 rounded-xl my-1 transition-all active:scale-95 overflow-hidden ${
              isActive
                ? 'text-[#779954] font-bold bg-[#779954]/10'
                : 'text-[var(--chess-text-secondary)] hover:text-[var(--chess-text-primary)] font-medium'
            }`}
          >
            {/* Top active pill bar */}
            {isActive && (
              <span className="absolute top-0 w-7 h-0.5 rounded-full bg-[#779954]" />
            )}

            <div className="relative flex items-center justify-center">
              <Icon className="w-5 h-5 shrink-0" />
              {showBadge && item.id === 'games' && (
                <span className="absolute -top-1.5 -right-3.5 px-1 min-w-[16px] h-4 rounded-full bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] text-[9px] font-mono-tabular font-bold text-[var(--chess-text-secondary)] flex items-center justify-center">
                  {savedGamesCount > 99 ? '99+' : savedGamesCount}
                </span>
              )}
              {showBadge && (item.id === 'analyzer' || item.id === 'repertoire') && (
                <span className="absolute -top-1 -right-2.5 px-1 rounded bg-[#F39C36] text-white text-[8px] font-mono-tabular font-bold">
                  UCI
                </span>
              )}
            </div>

            <span className="text-[10px] sm:text-[11px] leading-tight whitespace-nowrap truncate max-w-full px-1">
              {item.label}
            </span>
          </button>
        );
      })}
    </nav>
  );
};
