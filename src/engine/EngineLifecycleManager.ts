import { useEffect, useState } from 'react';
import { stockfishClient } from '../core/engine/StockfishClient';
import { repertoireStockfishClient } from '../core/repertoire/RepertoireAnalyzer';
import { useGameStore } from '../stores/useGameStore';
import { useRepertoireStore } from '../stores/useRepertoireStore';
import { useSettingsStore } from '../stores/useSettingsStore';
import { findNodeById } from '../core/chess/VariationTree';

export interface MobileSystemState {
  isOnline: boolean;
  appVisibility: 'active' | 'background';
  wasPausedByBackground: boolean;
}

/**
 * React hook that manages Android / mobile system lifecycle events:
 * 1. Pauses heavy Stockfish batch operations or live searches when the app goes to background
 *    and persists the current game + repertoire queue so no progress is lost.
 * 2. Restores interactive evaluation when the user brings the app back to foreground.
 * 3. Monitors network connectivity (`online` / `offline`) for Chess.com API and Lichess Explorer.
 */
export function useMobileSystemLifecycle(): MobileSystemState {
  const [isOnline, setIsOnline] = useState<boolean>(() =>
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [appVisibility, setAppVisibility] = useState<'active' | 'background'>('active');
  const [wasPausedByBackground, setWasPausedByBackground] = useState(false);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    const handleVisibilityChange = () => {
      if (typeof document === 'undefined') return;
      if (document.visibilityState === 'hidden') {
        setAppVisibility('background');
        // Always persist current game when leaving or minimizing the app
        useGameStore.getState().persistCurrentGame().catch(() => {});

        // Stop live idle search to save mobile battery & CPU in background
        const gameStore = useGameStore.getState();
        if (!gameStore.isBatchAnalyzing) {
          stockfishClient.stop();
        }

        // If repertoire batch analysis is running, pause it safely so task queue is saved to DB
        const repStore = useRepertoireStore.getState();
        if (repStore.engineTaskStatus === 'running') {
          repStore.pauseEngineAnalysis();
          setWasPausedByBackground(true);
        }
      } else {
        setAppVisibility('active');
        // Resume interactive position evaluation if not batch analyzing
        const gameStore = useGameStore.getState();
        if (!gameStore.isBatchAnalyzing) {
          const currentNode =
            findNodeById(gameStore.rootNode, gameStore.currentNodeId) || gameStore.rootNode;
          const cfg = useSettingsStore.getState().settings.engineConfig;
          stockfishClient.analyzePosition(currentNode.fen, cfg.depth, cfg.movetimeMs);
        }
      }
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  return {
    isOnline,
    appVisibility,
    wasPausedByBackground,
  };
}

/**
 * Safely restarts both main and repertoire Stockfish instances if the user requests an engine reset.
 */
export async function resetAllStockfishInstances(): Promise<void> {
  await stockfishClient.restartWorker();
  repertoireStockfishClient.stop();
}
