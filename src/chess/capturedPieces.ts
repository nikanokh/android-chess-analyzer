import { Chess, PieceSymbol } from 'chess.js';
import { parsePgnToTree } from '../services/PgnService/pgnService';

export interface CapturedPiecesSummary {
  /** White pieces that have been captured by Black (displayed next to Black player) */
  capturedWhitePieces: PieceSymbol[];
  /** Black pieces that have been captured by White (displayed next to White player) */
  capturedBlackPieces: PieceSymbol[];
  /** Material balance from White's POV (+3 means White is ahead by 3 points, -2 means Black is ahead by 2) */
  materialDiffWhitePov: number;
  whiteMaterialScore: number;
  blackMaterialScore: number;
}

const STARTING_COUNTS: Record<Exclude<PieceSymbol, 'k'>, number> = {
  q: 1,
  r: 2,
  b: 2,
  n: 2,
  p: 8,
};

const PIECE_VALUES: Record<Exclude<PieceSymbol, 'k'>, number> = {
  q: 9,
  r: 5,
  b: 3,
  n: 3,
  p: 1,
};

const DISPLAY_ORDER: Array<Exclude<PieceSymbol, 'k'>> = ['q', 'r', 'b', 'n', 'p'];

/**
 * Calculates captured pieces and material advantage for any valid FEN string.
 * Handles promotions accurately so promoted pieces do not produce negative captured counts.
 */
export function calculateCapturedPiecesFromFen(fen: string): CapturedPiecesSummary {
  const currentWhite: Record<Exclude<PieceSymbol, 'k'>, number> = { q: 0, r: 0, b: 0, n: 0, p: 0 };
  const currentBlack: Record<Exclude<PieceSymbol, 'k'>, number> = { q: 0, r: 0, b: 0, n: 0, p: 0 };

  try {
    const chess = new Chess(fen);
    const board = chess.board();
    for (let r = 0; r < 8; r++) {
      for (let c = 0; c < 8; c++) {
        const cell = board[r][c];
        if (!cell || cell.type === 'k') continue;
        if (cell.color === 'w') {
          currentWhite[cell.type]++;
        } else {
          currentBlack[cell.type]++;
        }
      }
    }
  } catch {
    return {
      capturedWhitePieces: [],
      capturedBlackPieces: [],
      materialDiffWhitePov: 0,
      whiteMaterialScore: 39,
      blackMaterialScore: 39,
    };
  }

  const capturedWhitePieces: PieceSymbol[] = [];
  const capturedBlackPieces: PieceSymbol[] = [];
  let whiteMaterialScore = 0;
  let blackMaterialScore = 0;

  for (const type of DISPLAY_ORDER) {
    whiteMaterialScore += currentWhite[type] * PIECE_VALUES[type];
    blackMaterialScore += currentBlack[type] * PIECE_VALUES[type];

    const missingWhite = Math.max(0, STARTING_COUNTS[type] - currentWhite[type]);
    for (let i = 0; i < missingWhite; i++) {
      capturedWhitePieces.push(type);
    }

    const missingBlack = Math.max(0, STARTING_COUNTS[type] - currentBlack[type]);
    for (let i = 0; i < missingBlack; i++) {
      capturedBlackPieces.push(type);
    }
  }

  return {
    capturedWhitePieces,
    capturedBlackPieces,
    materialDiffWhitePov: whiteMaterialScore - blackMaterialScore,
    whiteMaterialScore,
    blackMaterialScore,
  };
}

export interface ParsedPgnPreviewItem {
  index: number;
  rawPgn: string;
  whiteName: string;
  blackName: string;
  whiteRating?: number;
  blackRating?: number;
  result: string;
  date: string;
  event: string;
  eco?: string;
  movesCount: number;
  isValid: boolean;
  errorReason?: string;
}

/**
 * Splits a multi-game PGN file into individual game chunks and validates each game for preview before import.
 */
export function parseMultiGamePgnPreview(rawText: string): ParsedPgnPreviewItem[] {
  const normalized = rawText.replace(/\r\n/g, '\n').trim();
  if (!normalized) return [];

  // Split whenever a new [Event "..."] header starts after movetext
  const rawChunks = normalized
    .split(/\n(?=\[Event\s+")/g)
    .map((c) => c.trim())
    .filter(Boolean);

  const chunks = rawChunks.length > 0 ? rawChunks : [normalized];

  return chunks.map((chunk, idx) => {
    try {
      const parsed = parsePgnToTree(chunk, 'PGN');
      // Count mainline plies
      let plies = 0;
      let curr = parsed.root;
      while (curr.children.length > 0) {
        plies++;
        curr = curr.children[0];
      }

      if (plies === 0 && !chunk.includes('[FEN')) {
        return {
          index: idx,
          rawPgn: chunk,
          whiteName: parsed.metadata.whiteName || 'Белые',
          blackName: parsed.metadata.blackName || 'Чёрные',
          result: parsed.metadata.result || '*',
          date: parsed.metadata.date || '',
          event: parsed.metadata.event || 'Партия PGN',
          movesCount: 0,
          isValid: false,
          errorReason: 'Не найдено корректных ходов в тексте PGN',
        };
      }

      return {
        index: idx,
        rawPgn: chunk,
        whiteName: parsed.metadata.whiteName,
        blackName: parsed.metadata.blackName,
        whiteRating: parsed.metadata.whiteRating,
        blackRating: parsed.metadata.blackRating,
        result: parsed.metadata.result,
        date: parsed.metadata.date,
        event: parsed.metadata.event || 'Импорт PGN',
        eco: parsed.metadata.eco,
        movesCount: Math.ceil(plies / 2),
        isValid: true,
      };
    } catch (err) {
      return {
        index: idx,
        rawPgn: chunk,
        whiteName: 'Неизвестно',
        blackName: 'Неизвестно',
        result: '*',
        date: '',
        event: `Запись #${idx + 1}`,
        movesCount: 0,
        isValid: false,
        errorReason: err instanceof Error ? err.message : 'Ошибка синтаксиса PGN',
      };
    }
  });
}
