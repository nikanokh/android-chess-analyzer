import React, { useEffect, useState } from 'react';
import { Loader2, X } from 'lucide-react';
import { PieceSymbol } from 'chess.js';
import { PIECE_ASSETS } from '../../utils/assets';

export type TouchButtonVariant =
  | 'primary'
  | 'secondary'
  | 'outline'
  | 'ghost'
  | 'danger'
  | 'accent';

export type TouchButtonSize = 'sm' | 'md' | 'lg';

export interface TouchButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: TouchButtonVariant;
  size?: TouchButtonSize;
  isLoading?: boolean;
  isSelected?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  fullWidth?: boolean;
}

/**
 * Unified Mobile Touch Button with explicit states:
 * - Default
 * - Pressed (active scale + highlight)
 * - Disabled
 * - Loading (spinner + prevents duplicate clicks)
 * - Selected (highlighted ring/surface)
 * Enforces minimum 44dp touch hitbox on mobile viewports.
 */
export const TouchButton: React.FC<TouchButtonProps> = ({
  variant = 'secondary',
  size = 'md',
  isLoading = false,
  isSelected = false,
  leftIcon,
  rightIcon,
  fullWidth = false,
  disabled,
  className = '',
  children,
  onClick,
  ...rest
}) => {
  const isDisabled = Boolean(disabled || isLoading);

  const sizeClasses: Record<TouchButtonSize, string> = {
    sm: 'min-h-[40px] sm:min-h-[36px] px-3 py-1.5 text-xs gap-1.5 rounded-lg',
    md: 'min-h-[44px] px-4 py-2 text-xs sm:text-sm gap-2 rounded-xl',
    lg: 'min-h-[48px] px-5 py-2.5 text-sm gap-2.5 rounded-xl',
  };

  const variantClasses: Record<TouchButtonVariant, string> = {
    primary: isSelected
      ? 'bg-[#62934B] text-white ring-2 ring-[#779954] shadow-sm'
      : 'bg-[#779954] hover:bg-[#62934B] active:bg-[#558240] text-white shadow-xs',
    secondary: isSelected
      ? 'bg-[#779954]/20 text-[#779954] border border-[#779954] font-bold'
      : 'bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)]/70 active:bg-[var(--chess-border)] text-[var(--chess-text-primary)] border border-[var(--chess-border)]',
    outline: isSelected
      ? 'bg-[#779954]/15 text-[#779954] border-2 border-[#779954]'
      : 'bg-transparent hover:bg-[var(--chess-bg-secondary)] text-[var(--chess-text-primary)] border border-[var(--chess-border)]',
    ghost: isSelected
      ? 'bg-[#779954]/20 text-[#779954]'
      : 'bg-transparent hover:bg-[var(--chess-bg-secondary)] text-[var(--chess-text-secondary)] hover:text-[var(--chess-text-primary)]',
    danger:
      'bg-[#E05252] hover:bg-[#cc4444] active:bg-[#b83b3b] text-white shadow-xs',
    accent: isSelected
      ? 'bg-[#2B93C7] text-white ring-2 ring-[#36A9E1]'
      : 'bg-[#36A9E1] hover:bg-[#2B93C7] text-white shadow-xs',
  };

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (isDisabled) {
      e.preventDefault();
      return;
    }
    onClick?.(e);
  };

  return (
    <button
      type="button"
      disabled={isDisabled}
      onClick={handleClick}
      className={`inline-flex items-center justify-center font-semibold transition-all duration-150 select-none whitespace-nowrap max-w-full min-w-0 overflow-hidden active:scale-[0.97] disabled:opacity-45 disabled:pointer-events-none disabled:active:scale-100 ${
        sizeClasses[size]
      } ${variantClasses[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...rest}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : (
        leftIcon && <span className="shrink-0 flex items-center">{leftIcon}</span>
      )}
      {children && <span className="truncate min-w-0">{children}</span>}
      {!isLoading && rightIcon && (
        <span className="shrink-0 flex items-center">{rightIcon}</span>
      )}
    </button>
  );
};

export interface TouchSwitchProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: string;
  description?: string;
  disabled?: boolean;
}

/**
 * Android-style accessible Toggle Switch with >= 48dp row touch target.
 */
export const TouchSwitch: React.FC<TouchSwitchProps> = ({
  checked,
  onChange,
  label,
  description,
  disabled = false,
}) => {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={() => !disabled && onChange(!checked)}
      className="w-full max-w-full min-h-[52px] px-3.5 py-2.5 rounded-xl bg-[var(--chess-bg-secondary)] hover:bg-[var(--chess-border)]/40 border border-[var(--chess-border)] flex items-center justify-between gap-3 text-left transition-colors active:scale-[0.99] disabled:opacity-50 overflow-hidden"
    >
      <div className="min-w-0 flex-1">
        <div className="text-xs sm:text-sm font-semibold text-[var(--chess-text-primary)] break-words">
          {label}
        </div>
        {description && (
          <div className="text-[11px] text-[var(--chess-text-secondary)] mt-0.5 leading-snug break-words">
            {description}
          </div>
        )}
      </div>

      <div
        className={`w-12 h-7 rounded-full p-0.5 transition-colors duration-200 shrink-0 flex items-center ${
          checked ? 'bg-[#779954]' : 'bg-[var(--chess-border)]'
        }`}
      >
        <div
          className={`w-6 h-6 rounded-full bg-white shadow-md transform transition-transform duration-200 ${
            checked ? 'translate-x-5' : 'translate-x-0'
          }`}
        />
      </div>
    </button>
  );
};

export interface SegmentedOption<T extends string> {
  id: T;
  label: string;
  icon?: React.ReactNode;
  badge?: string | number;
}

export interface SegmentedControlProps<T extends string> {
  options: SegmentedOption<T>[];
  value: T;
  onChange: (val: T) => void;
  size?: 'sm' | 'md';
}

export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  size = 'md',
}: SegmentedControlProps<T>) {
  return (
    <div className="flex flex-wrap sm:flex-nowrap items-center bg-[var(--chess-bg-secondary)] border border-[var(--chess-border)] rounded-xl p-1 gap-1 w-full max-w-full overflow-hidden">
      {options.map((opt) => {
        const active = opt.id === value;
        return (
          <button
            key={opt.id}
            type="button"
            onClick={() => onChange(opt.id)}
            className={`flex-1 min-w-0 overflow-hidden ${
              size === 'sm' ? 'min-h-[36px] px-2 py-1 text-xs' : 'min-h-[40px] px-2.5 py-1.5 text-xs sm:text-sm'
            } rounded-lg font-semibold transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
              active
                ? 'bg-[#779954] text-white shadow-xs'
                : 'text-[var(--chess-text-secondary)] hover:text-[var(--chess-text-primary)]'
            }`}
          >
            {opt.icon && <span className="shrink-0 flex items-center">{opt.icon}</span>}
            <span className="truncate min-w-0">{opt.label}</span>
            {opt.badge !== undefined && (
              <span
                className={`px-1.5 py-0.2 rounded text-[10px] font-mono-tabular shrink-0 ${
                  active ? 'bg-black/20 text-white' : 'bg-[var(--chess-bg-primary)] text-[var(--chess-text-secondary)]'
                }`}
              >
                {opt.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}

export interface BottomSheetModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  icon?: React.ReactNode;
  children: React.ReactNode;
  footer?: React.ReactNode;
  maxWidthClass?: string;
}

/**
 * Mobile Bottom Sheet / Modal Dialog with drag-handle, backdrop blur,
 * touch swipe-down to dismiss, and Escape / Android Back key handling.
 */
export const BottomSheetModal: React.FC<BottomSheetModalProps> = ({
  isOpen,
  onClose,
  title,
  subtitle,
  icon,
  children,
  footer,
  maxWidthClass = 'max-w-2xl',
}) => {
  const [touchStartY, setTouchStartY] = useState<number | null>(null);
  const [dragOffsetY, setDragOffsetY] = useState(0);

  useEffect(() => {
    if (!isOpen) {
      setDragOffsetY(0);
      return;
    }
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/65 backdrop-blur-xs flex items-end sm:items-center justify-center sm:p-4 animate-fadeIn"
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          transform: dragOffsetY > 0 ? `translateY(${dragOffsetY}px)` : undefined,
        }}
        className={`bg-[var(--chess-bg-primary)] border-t sm:border border-[var(--chess-border)] rounded-t-3xl sm:rounded-2xl shadow-2xl w-full ${maxWidthClass} max-h-[90dvh] flex flex-col overflow-hidden transition-transform duration-150`}
      >
        {/* Mobile Drag Handle */}
        <div
          onTouchStart={(e) => setTouchStartY(e.touches[0].clientY)}
          onTouchMove={(e) => {
            if (touchStartY === null) return;
            const delta = e.touches[0].clientY - touchStartY;
            if (delta > 0) setDragOffsetY(delta);
          }}
          onTouchEnd={() => {
            if (dragOffsetY > 90) {
              onClose();
            }
            setDragOffsetY(0);
            setTouchStartY(null);
          }}
          className="pt-2.5 pb-1 sm:hidden cursor-grab active:cursor-grabbing flex flex-col items-center"
        >
          <div className="w-11 h-1.5 rounded-full bg-[var(--chess-border)]" />
        </div>

        {/* Sheet Header */}
        <div className="px-4 py-3 border-b border-[var(--chess-border)] flex items-center justify-between gap-3 shrink-0">
          <div className="flex items-center gap-2.5 min-w-0">
            {icon && <span className="text-[#779954] shrink-0">{icon}</span>}
            <div className="min-w-0">
              <h3 className="text-sm sm:text-base font-bold text-[var(--chess-text-primary)] truncate">
                {title}
              </h3>
              {subtitle && (
                <p className="text-[11px] text-[var(--chess-text-secondary)] truncate">
                  {subtitle}
                </p>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Закрыть"
            className="min-w-[44px] min-h-[44px] rounded-xl flex items-center justify-center text-[var(--chess-text-secondary)] hover:text-[var(--chess-text-primary)] hover:bg-[var(--chess-bg-secondary)] transition-colors shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Sheet Scrollable Body */}
        <div className="flex-1 min-h-0 overflow-y-auto p-4">{children}</div>

        {/* Optional Pinned Footer */}
        {footer && (
          <div className="px-4 py-3 border-t border-[var(--chess-border)] bg-[var(--chess-bg-secondary)] shrink-0">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
};

export interface CapturedPiecesStripProps {
  /** Color of the captured pieces to display ('w' = white pieces captured by Black, 'b' = black pieces captured by White) */
  capturedPieceColor: 'w' | 'b';
  pieces: PieceSymbol[];
  /** Positive material advantage for this player (e.g. 3 means +3) */
  materialAdvantage: number;
}

/**
 * Renders captured chess pieces with overlapping icons and "+N" material advantage readout.
 */
export const CapturedPiecesStrip: React.FC<CapturedPiecesStripProps> = ({
  capturedPieceColor,
  pieces,
  materialAdvantage,
}) => {
  if (pieces.length === 0 && materialAdvantage <= 0) {
    return null;
  }

  return (
    <div className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-[var(--chess-bg-secondary)]/80">
      {pieces.length > 0 && (
        <div className="flex items-center -space-x-1.5 overflow-hidden max-w-[130px] sm:max-w-[180px]">
          {pieces.slice(0, 12).map((p, idx) => {
            const key = `${capturedPieceColor}${p.toUpperCase()}`;
            return (
              <img
                key={`${key}_${idx}`}
                src={PIECE_ASSETS[key]}
                alt={key}
                referrerPolicy="no-referrer"
                className="w-3.5 h-3.5 sm:w-4 sm:h-4 object-contain drop-shadow-xs shrink-0"
              />
            );
          })}
        </div>
      )}
      {materialAdvantage > 0 && (
        <span className="text-[10px] sm:text-[11px] font-mono-tabular font-bold text-[#779954] ml-0.5">
          +{materialAdvantage}
        </span>
      )}
    </div>
  );
};
