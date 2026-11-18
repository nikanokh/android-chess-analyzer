/**
 * Preliminary Project Audit & React Native + Expo Migration Specification
 * Documents what was preserved 1:1, what was adapted for React Native / Android,
 * what was built from scratch, library replacements, and mitigated edge cases.
 */

export interface AuditTableEntry {
  module: string;
  category: 'preserved' | 'adapted' | 'rewritten' | 'library_replacement' | 'risk_mitigation';
  sourcePath: string;
  targetPath: string;
  details: string;
  status: 'completed' | 'verified';
}

export const MIGRATION_AUDIT_TABLE: AuditTableEntry[] = [
  // 1. Перенесено практически без изменений (Preserved 1:1)
  {
    module: 'Дерево вариантов и парсер ходов',
    category: 'preserved',
    sourcePath: 'src/core/chess/VariationTree.ts',
    targetPath: 'src/core/chess/VariationTree.ts',
    details: 'Полная поддержка ветвлений, комментариев, SAN/UCI конвертации, экспорт и импорт PGN.',
    status: 'verified',
  },
  {
    module: 'Дебютная книга ECO и транспозиции по FEN',
    category: 'preserved',
    sourcePath: 'src/core/chess/OpeningBook.ts',
    targetPath: 'src/core/chess/OpeningBook.ts',
    details: 'Определение кода ECO, названия дебюта, последнего теоретического хода и выхода из книги.',
    status: 'verified',
  },
  {
    module: 'Алгоритм точности и вероятности победы (Win% / CPL)',
    category: 'preserved',
    sourcePath: 'src/core/analysis/AccuracyCalculator.ts',
    targetPath: 'src/core/analysis/AccuracyCalculator.ts',
    details: 'Сигмоида пересчёта сантипешек в Win%, гармоническая и взвешенная точность белых и чёрных.',
    status: 'verified',
  },
  {
    module: 'Классификатор ходов (11 категорий от Brilliant до Blunder)',
    category: 'preserved',
    sourcePath: 'src/core/analysis/MoveAnalyzer.ts',
    targetPath: 'src/core/analysis/MoveAnalyzer.ts',
    details: 'Определение жертв фигур (Brilliant), единственных ходов (Great), ошибок, зевков и упущенных шансов.',
    status: 'verified',
  },
  {
    module: 'Аналитика дебютного репертуара и проблемных линий',
    category: 'preserved',
    sourcePath: 'src/core/repertoire/*',
    targetPath: 'src/core/repertoire/*',
    details: 'Агрегация W/D/L, дерево ходов репертуара, поиск систематических ошибок и потери перевеса.',
    status: 'verified',
  },
  {
    module: 'TypeScript-интерфейсы и модели данных',
    category: 'preserved',
    sourcePath: 'src/types/chess.ts, src/types/repertoire.ts',
    targetPath: 'src/types/chess.ts, src/types/repertoire.ts',
    details: 'Строгая типизация узлов дерева, оценок движка, метаданных партий и кэша репертуара.',
    status: 'verified',
  },

  // 2. Адаптировано под React Native / Android UX
  {
    module: 'Клиент Stockfish UCI и управление потоками',
    category: 'adapted',
    sourcePath: 'src/core/engine/StockfishClient.ts',
    targetPath: 'src/engine/EngineLifecycleManager.ts & mobile/engine/StockfishNativeEngine.ts',
    details: 'Однопоточный режим с movetime-бюджетом, защита от дублирования инстансов, авто-пауза при сворачивании (AppState / visibilitychange) и восстановление очереди.',
    status: 'verified',
  },
  {
    module: 'Хранилища состояния Zustand (Game, Settings, Repertoire)',
    category: 'adapted',
    sourcePath: 'src/stores/*.ts',
    targetPath: 'src/stores/*.ts',
    details: 'Тёмная тема по умолчанию, сохранение истории действий пользователя, автосохранение текущей партии при смене экрана.',
    status: 'verified',
  },
  {
    module: 'Интеграция с Chess.com PubAPI и Lichess Explorer',
    category: 'adapted',
    sourcePath: 'src/services/ChessComApi/chessComService.ts',
    targetPath: 'src/services/ChessComApi/chessComService.ts',
    details: 'Экспоненциальный backoff, кэширование архивов, дедупликация по UUID/URL, корректная работа в офлайн-режиме.',
    status: 'verified',
  },

  // 3. Реализовано заново для мобильного UX
  {
    module: 'Нижняя навигация (Bottom Tab Navigation) + Боковое меню (Drawer)',
    category: 'rewritten',
    sourcePath: 'src/App.tsx (веб-шапка)',
    targetPath: 'src/components/Navigation/BottomTabBar.tsx & MobileDrawer.tsx & mobile/app/(tabs)/_layout.tsx',
    details: '5 основных разделов (Главная, Партии, Анализ, Репертуар, Настройки), свайп-меню Drawer, поддержка системной кнопки «Назад» Android.',
    status: 'verified',
  },
  {
    module: 'Экран «Главная» (Mobile Home Dashboard)',
    category: 'rewritten',
    sourcePath: 'Отсутствовал (сразу открывалась доска)',
    targetPath: 'src/features/home/HomeScreen.tsx & mobile/app/(tabs)/index.tsx',
    details: 'Сводная статистика игрока, карточка быстрого возврата к анализу, быстрый импорт PGN, профиль Chess.com, последние партии и журнал действий.',
    status: 'verified',
  },
  {
    module: 'Экран «Архив партий» с виртуализацией и мульти-импортом PGN',
    category: 'rewritten',
    sourcePath: 'src/components/Modals/SavedGamesModal.tsx',
    targetPath: 'src/features/games/GamesArchiveScreen.tsx & mobile/app/(tabs)/games.tsx',
    details: 'Оптимизированный список партий, поиск, фильтры по платформе/результату/анализу, сортировка, пакетный импорт и системный экспорт/Share PGN.',
    status: 'verified',
  },
  {
    module: 'Компонент взятых фигур и материального баланса',
    category: 'rewritten',
    sourcePath: 'Отсутствовал',
    targetPath: 'src/chess/capturedPieces.ts & src/components/ui/MobilePrimitives.tsx',
    details: 'Точный подсчёт взятых фигур из любого FEN и отображение материального перевеса (+1, +3 и т.д.) возле имени каждого игрока.',
    status: 'verified',
  },
  {
    module: 'Мобильная дизайн-система и Touch-компоненты (44–48dp)',
    category: 'rewritten',
    sourcePath: 'Разрозненные кнопки',
    targetPath: 'src/components/ui/MobilePrimitives.tsx',
    details: 'Единые компоненты TouchButton (Default, Pressed, Disabled, Loading, Selected), TouchSwitch, SegmentedControl, BottomSheetModal.',
    status: 'verified',
  },

  // 4. Замена библиотек для React Native + Expo
  {
    module: 'Локальная БД: Dexie (IndexedDB) → expo-sqlite + миграционный мост',
    category: 'library_replacement',
    sourcePath: 'dexie / dexie-react-hooks',
    targetPath: 'expo-sqlite (src/database/sqliteSchema.ts & mobile/database/sqliteClient.ts)',
    details: 'Реляционная схема SQLite v3 с индексами, автоматической миграцией существующих партий и кэша репертуара без потери данных.',
    status: 'verified',
  },
  {
    module: 'Файловый ввод/вывод: HTML File Input / Blob → Expo DocumentPicker + FileSystem + Sharing',
    category: 'library_replacement',
    sourcePath: '<input type="file"> / URL.createObjectURL',
    targetPath: 'expo-document-picker, expo-file-system, expo-sharing + Web Share API',
    details: 'Системный выбор .pgn файлов без запроса полного доступа к хранилищу Android, экспорт и отправка через Android Share Sheet.',
    status: 'verified',
  },

  // 5. Потенциальные проблемы и их решения
  {
    module: 'Зависание однопоточного Stockfish и отсутствие SharedArrayBuffer на Android',
    category: 'risk_mitigation',
    sourcePath: 'Web Worker pthreads',
    targetPath: 'Single-threaded UCI + movetime cap',
    details: 'Автоматическое определение однопоточной сборки (max threads = 1) с установкой безопасного movetime бюджета и перезапуском при таймауте.',
    status: 'verified',
  },
  {
    module: 'Прерывание анализа при сворачивании приложения Android',
    category: 'risk_mitigation',
    sourcePath: 'Непрерывный цикл в памяти',
    targetPath: 'src/engine/EngineLifecycleManager.ts & repertoireTaskState',
    details: 'Сохранение прогресса пакетного анализа в БД после каждого хода/партии и автоматическая постановка на паузу с предложением продолжить.',
    status: 'verified',
  },
];

export const EXPO_ANDROID_BUILD_SPEC = {
  appName: 'Chess Insight Analyzer',
  packageName: 'com.nikanokh.chessinsightanalyzermobile',
  expoSdkVersion: 'Kotlin 2.0.21 + Jetpack Compose (Material 3)',
  minSdkVersion: 24,
  targetSdkVersion: 35,
  permissions: ['android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE', 'android.permission.VIBRATE'],
  easProfiles: {
    previewApk: 'cd mobile && .\\build-apk.ps1 -Mode debug',
    productionAab: 'cd mobile && .\\gradlew.bat bundleRelease',
    localGradleApk: 'cd mobile && .\\build-apk.ps1 -Mode release',
  },
};
