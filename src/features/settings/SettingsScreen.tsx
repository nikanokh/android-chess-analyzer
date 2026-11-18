import React, { useState } from 'react';
import { useSettingsStore } from '../../stores/useSettingsStore';
import { useGameStore } from '../../stores/useGameStore';
import { useRepertoireStore } from '../../stores/useRepertoireStore';
import {
  SegmentedControl,
  TouchButton,
  TouchSwitch,
} from '../../components/ui/MobilePrimitives';
import {
  EXPO_ANDROID_BUILD_SPEC,
  MIGRATION_AUDIT_TABLE,
} from '../../constants/migrationAudit';
import {
  SQLITE_DATABASE_NAME,
  SQLITE_SCHEMA_VERSION,
} from '../../database/sqliteSchema';
import {
  CheckCircle2,
  Cpu,
  Database,
  Layers,
  Palette,
  RotateCcw,
  Settings,
  Smartphone,
  Terminal,
  Trash2,
} from 'lucide-react';

const BOARD_COLOR_PRESETS = [
  { name: 'Классический зелёный (Chess.com)', light: '#EEEED2', dark: '#769656' },
  { name: 'Турнирное дерево', light: '#F0D9B5', dark: '#B58863' },
  { name: 'Морской лёд', light: '#DEE3E6', dark: '#8CA2AD' },
  { name: 'Графитовый минимализм', light: '#E6E6E6', dark: '#6E7582' },
];

type SettingsSectionTab = 'board' | 'engine' | 'android_audit';

export const SettingsScreen: React.FC = () => {
  const [sectionTab, setSectionTab] = useState<SettingsSectionTab>('board');

  const settings = useSettingsStore((s) => s.settings);
  const updateSettings = useSettingsStore((s) => s.updateSettings);
  const updateEngineConfig = useSettingsStore((s) => s.updateEngineConfig);
  const updateThresholds = useSettingsStore((s) => s.updateThresholds);
  const resetSettings = useSettingsStore((s) => s.resetSettings);

  const recalculateWithThresholds = useGameStore((s) => s.recalculateWithThresholds);
  const savedGames = useGameStore((s) => s.savedGames);

  const cacheStats = useRepertoireStore((s) => s.cacheStats);
  const clearAllRepertoireCache = useRepertoireStore((s) => s.clearAllRepertoireCache);

  const handleThresholdChange = (key: keyof typeof settings.thresholds, val: number) => {
    const next = { ...settings.thresholds, [key]: val };
    updateThresholds({ [key]: val });
    recalculateWithThresholds(next);
  };

  return (
    <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden p-3 sm:p-5 space-y-4 max-w-5xl w-full mx-auto">
      {/* Header & Section Switcher */}
      <div className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h1 className="text-base sm:text-lg font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
              <Settings className="w-5 h-5 text-[#779954]" />
              <span>Настройки приложения и движка Stockfish</span>
            </h1>
            <p className="text-xs text-[var(--chess-text-secondary)] mt-0.5">
              Все параметры автоматически сохраняются в локальной базе данных
            </p>
          </div>

          <TouchButton
            variant="outline"
            size="sm"
            leftIcon={<RotateCcw className="w-3.5 h-3.5 text-[#E05252]" />}
            onClick={resetSettings}
          >
            Сбросить по умолчанию
          </TouchButton>
        </div>

        <SegmentedControl
          value={sectionTab}
          onChange={setSectionTab}
          options={[
            {
              id: 'board',
              label: 'Доска и интерфейс',
              icon: <Palette className="w-4 h-4" />,
            },
            {
              id: 'engine',
              label: 'Stockfish UCI и CPL',
              icon: <Cpu className="w-4 h-4" />,
            },
            {
              id: 'android_audit',
              label: 'БД SQLite & Android APK',
              icon: <Smartphone className="w-4 h-4" />,
            },
          ]}
        />
      </div>

      {/* TAB 1: Board & Interface Settings */}
      {sectionTab === 'board' && (
        <div className="space-y-4">
          {/* Theme & Piece Set */}
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-4">
            <h2 className="text-sm font-bold text-[var(--chess-text-primary)]">
              Оформление и набор фигур
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <span className="text-xs text-[var(--chess-text-secondary)] block">
                  Цветовая тема интерфейса
                </span>
                <SegmentedControl
                  size="sm"
                  value={settings.theme}
                  onChange={(val) => updateSettings({ theme: val })}
                  options={[
                    { id: 'dark', label: 'Тёмная (Android Default)' },
                    { id: 'light', label: 'Светлая' },
                  ]}
                />
              </div>

              <div className="space-y-1.5">
                <span className="text-xs text-[var(--chess-text-secondary)] block">
                  Стиль шахматных фигур
                </span>
                <SegmentedControl
                  size="sm"
                  value={settings.pieceSet}
                  onChange={(val) => updateSettings({ pieceSet: val })}
                  options={[
                    { id: 'neo', label: 'Neo (Chess.com)' },
                    { id: 'classic_svg', label: 'Classic Vector SVG' },
                  ]}
                />
              </div>
            </div>

            {/* Board Color Presets */}
            <div className="space-y-2 pt-2 border-t border-[var(--chess-border)]">
              <span className="text-xs text-[var(--chess-text-secondary)] block">
                Палитра клеток шахматной доски
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {BOARD_COLOR_PRESETS.map((preset) => {
                  const active =
                    settings.darkSquareColor.toLowerCase() === preset.dark.toLowerCase();
                  return (
                    <button
                      key={preset.name}
                      type="button"
                      onClick={() =>
                        updateSettings({
                          lightSquareColor: preset.light,
                          darkSquareColor: preset.dark,
                        })
                      }
                      className={`min-h-[48px] p-3 rounded-xl border flex items-center gap-3 text-left transition-all active:scale-[0.99] ${
                        active
                          ? 'border-[#779954] bg-[#779954]/15 font-bold'
                          : 'border-[var(--chess-border)] bg-[var(--chess-bg-secondary)]'
                      }`}
                    >
                      <span className="flex shrink-0 rounded-md overflow-hidden border border-black/25">
                        <span
                          style={{ backgroundColor: preset.light }}
                          className="w-5 h-5"
                        />
                        <span
                          style={{ backgroundColor: preset.dark }}
                          className="w-5 h-5"
                        />
                      </span>
                      <span className="text-xs text-[var(--chess-text-primary)] truncate">
                        {preset.name}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </section>

          {/* Touch-Friendly Switches */}
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-3">
            <h2 className="text-sm font-bold text-[var(--chess-text-primary)]">
              Параметры шахматной доски и подсказок
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
              <TouchSwitch
                label="Плавная анимация ходов"
                description="Плавное перемещение фигур при навигации по ходам"
                checked={settings.enableAnimations}
                onChange={(val) => updateSettings({ enableAnimations: val })}
              />
              <TouchSwitch
                label="Подсветка последнего хода"
                description="Выделять начальную и конечную клетку сделанного хода"
                checked={settings.highlightLastMove}
                onChange={(val) => updateSettings({ highlightLastMove: val })}
              />
              <TouchSwitch
                label="Доступные ходы выбранной фигуры"
                description="Показывать точки и кольца взятия при касании фигуры"
                checked={settings.highlightLegalMoves}
                onChange={(val) => updateSettings({ highlightLegalMoves: val })}
              />
              <TouchSwitch
                label="Координаты доски (A–H, 1–8)"
                description="Пропорциональная разметка вертикалей и горизонталей"
                checked={settings.showCoordinates}
                onChange={(val) => updateSettings({ showCoordinates: val })}
              />
              <TouchSwitch
                label="Вертикальная шкала оценки"
                description="Показывать столбик перевеса Stockfish слева от доски"
                checked={settings.showEvalBar}
                onChange={(val) => updateSettings({ showEvalBar: val })}
              />
              <TouchSwitch
                label="Стрелки лучших ходов Stockfish"
                description="Отображать линии MultiPV прямо поверх доски"
                checked={settings.showEngineArrows}
                onChange={(val) => updateSettings({ showEngineArrows: val })}
              />
            </div>
          </section>
        </div>
      )}

      {/* TAB 2: Stockfish UCI & Thresholds */}
      {sectionTab === 'engine' && (
        <div className="space-y-4">
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-4">
            <h2 className="text-sm font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
              <Cpu className="w-4 h-4 text-[#779954]" />
              <span>Параметры движка Stockfish 16 UCI</span>
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Depth slider */}
              <div className="p-3.5 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-[var(--chess-text-primary)]">
                    Глубина расчёта (Depth)
                  </span>
                  <span className="font-mono-tabular font-bold text-[#779954]">
                    {settings.engineConfig.depth} полуходов
                  </span>
                </div>
                <input
                  type="range"
                  min={6}
                  max={26}
                  value={settings.engineConfig.depth}
                  onChange={(e) => updateEngineConfig({ depth: Number(e.target.value) })}
                  className="w-full h-2 accent-[#779954] cursor-pointer"
                />
                <div className="text-[11px] text-[var(--chess-text-secondary)]">
                  Рекомендуется 12–16 для быстрого мобильного анализа без перегрева CPU
                </div>
              </div>

              {/* MultiPV slider */}
              <div className="p-3.5 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-[var(--chess-text-primary)]">
                    Количество линий (MultiPV)
                  </span>
                  <span className="font-mono-tabular font-bold text-[#36A9E1]">
                    {settings.engineConfig.multiPv} вар.
                  </span>
                </div>
                <input
                  type="range"
                  min={1}
                  max={5}
                  value={settings.engineConfig.multiPv}
                  onChange={(e) => updateEngineConfig({ multiPv: Number(e.target.value) })}
                  className="w-full h-2 accent-[#36A9E1] cursor-pointer"
                />
                <div className="text-[11px] text-[var(--chess-text-secondary)]">
                  Число одновременно рассчитываемых альтернативных продолжений
                </div>
              </div>

              {/* Hash MB */}
              <div className="p-3.5 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] space-y-2">
                <span className="text-xs font-semibold text-[var(--chess-text-primary)] block">
                  Размер хэш-таблицы (Hash МБ)
                </span>
                <SegmentedControl
                  size="sm"
                  value={String(settings.engineConfig.hashMb)}
                  onChange={(val) => updateEngineConfig({ hashMb: Number(val) })}
                  options={[
                    { id: '16', label: '16 МБ' },
                    { id: '32', label: '32 МБ' },
                    { id: '64', label: '64 МБ' },
                    { id: '128', label: '128 МБ' },
                  ]}
                />
              </div>

              {/* Threads */}
              <div className="p-3.5 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] space-y-2">
                <span className="text-xs font-semibold text-[var(--chess-text-primary)] block">
                  Потоки процессора (Threads)
                </span>
                <SegmentedControl
                  size="sm"
                  value={String(settings.engineConfig.threads)}
                  onChange={(val) => updateEngineConfig({ threads: Number(val) })}
                  options={[
                    { id: '1', label: '1 поток (Экономия)' },
                    { id: '2', label: '2 потока' },
                    { id: '4', label: '4 потока' },
                  ]}
                />
              </div>
            </div>
          </section>

          {/* Move Classification CPL Thresholds */}
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-3">
            <h2 className="text-sm font-bold text-[var(--chess-text-primary)]">
              Пороги классификации ходов (в сантипешках CPL)
            </h2>
            <p className="text-xs text-[var(--chess-text-secondary)]">
              При изменении порогов уже проанализированная партия мгновенно переклассифицируется без повторного запуска Stockfish.
            </p>

            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              {[
                { key: 'bestMaxCpl', label: 'Лучший (≤)' },
                { key: 'excellentMaxCpl', label: 'Прекрасный (≤)' },
                { key: 'goodMaxCpl', label: 'Хороший (≤)' },
                { key: 'inaccuracyMaxCpl', label: 'Неточность (≤)' },
                { key: 'mistakeMaxCpl', label: 'Ошибка (≤)' },
              ].map((item) => (
                <div
                  key={item.key}
                  className="p-2.5 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)]"
                >
                  <label className="text-[11px] text-[var(--chess-text-secondary)] block mb-1">
                    {item.label}
                  </label>
                  <input
                    type="number"
                    min={0}
                    max={500}
                    value={settings.thresholds[item.key as keyof typeof settings.thresholds]}
                    onChange={(e) =>
                      handleThresholdChange(
                        item.key as keyof typeof settings.thresholds,
                        Number(e.target.value)
                      )
                    }
                    className="w-full min-h-[38px] px-2 rounded-lg bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] font-mono-tabular text-xs font-bold text-[var(--chess-text-primary)]"
                  />
                </div>
              ))}
            </div>
          </section>
        </div>
      )}

      {/* TAB 3: SQLite Storage, Audit Table & Android Expo APK Build */}
      {sectionTab === 'android_audit' && (
        <div className="space-y-4">
          {/* SQLite Database & Cache Summary */}
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h2 className="text-sm font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
                  <Database className="w-4 h-4 text-[#779954]" />
                  <span>Локальная БД: {SQLITE_DATABASE_NAME} (Схема v{SQLITE_SCHEMA_VERSION})</span>
                </h2>
                <p className="text-xs text-[var(--chess-text-secondary)] mt-0.5">
                  Партий в архиве: {savedGames.length} · Записей кэша дебютов:{' '}
                  {cacheStats.totalCachedGames} (со Stockfish: {cacheStats.engineAnalyzedGames})
                </p>
              </div>

              <TouchButton
                variant="danger"
                size="sm"
                leftIcon={<Trash2 className="w-3.5 h-3.5" />}
                onClick={() => clearAllRepertoireCache()}
              >
                Очистить кэш репертуара
              </TouchButton>
            </div>
          </section>

          {/* Android APK / AAB Build Commands */}
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-3">
            <h2 className="text-sm font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
              <Terminal className="w-4 h-4 text-[#36A9E1]" />
              <span>Конфигурация сборки Android APK / AAB (Expo SDK 53)</span>
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5 text-xs font-mono-tabular">
              <div className="p-3 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)]">
                <div className="text-[var(--chess-text-secondary)] text-[10px]">
                  Android Package
                </div>
                <div className="font-bold text-[var(--chess-text-primary)] mt-0.5">
                  {EXPO_ANDROID_BUILD_SPEC.packageName}
                </div>
              </div>
              <div className="p-3 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)]">
                <div className="text-[var(--chess-text-secondary)] text-[10px]">
                  Target / Min SDK
                </div>
                <div className="font-bold text-[var(--chess-text-primary)] mt-0.5">
                  API {EXPO_ANDROID_BUILD_SPEC.targetSdkVersion} / min API{' '}
                  {EXPO_ANDROID_BUILD_SPEC.minSdkVersion}
                </div>
              </div>
              <div className="p-3 rounded-xl bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)]">
                <div className="text-[var(--chess-text-secondary)] text-[10px]">
                  Сборка тестового APK
                </div>
                <div className="font-bold text-[#779954] mt-0.5 truncate">
                  {EXPO_ANDROID_BUILD_SPEC.easProfiles.previewApk}
                </div>
              </div>
            </div>
          </section>

          {/* Complete Migration Audit Table */}
          <section className="p-4 rounded-2xl bg-[var(--chess-bg-primary)] border border-[var(--chess-border)] space-y-3">
            <h2 className="text-sm font-bold text-[var(--chess-text-primary)] flex items-center gap-2">
              <Layers className="w-4 h-4 text-[#A88865]" />
              <span>Аудит миграции модулей на React Native + Expo</span>
            </h2>

            <div className="divide-y divide-[var(--chess-border)] rounded-xl border border-[var(--chess-border)] overflow-hidden text-xs">
              {MIGRATION_AUDIT_TABLE.map((row, idx) => (
                <div
                  key={idx}
                  className="p-3 bg-[var(--chess-bg-secondary)]/50 flex flex-col sm:flex-row sm:items-center justify-between gap-2"
                >
                  <div className="space-y-0.5 min-w-0 flex-1">
                    <div className="font-bold text-[var(--chess-text-primary)] flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-[#779954] shrink-0" />
                      <span>{row.module}</span>
                    </div>
                    <div className="text-[11px] text-[var(--chess-text-secondary)]">
                      {row.details}
                    </div>
                  </div>
                  <div className="text-[10px] font-mono-tabular text-[var(--chess-text-secondary)] shrink-0 sm:text-right">
                    <div>{row.targetPath}</div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}
    </div>
  );
};
