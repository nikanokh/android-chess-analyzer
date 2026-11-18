import { describe, it, expect } from 'vitest';
import {
  cpToWhiteWinProbability,
  calculateSingleMoveAccuracy,
  WIN_PROB_LOGISTIC_COEFFICIENT,
} from '../analysis/AccuracyCalculator';
import {
  classifyMoveFromEvaluations,
  DEFAULT_THRESHOLDS,
} from '../analysis/MoveAnalyzer';
import { openingBook } from '../chess/OpeningBook';
import { PIECE_ASSETS, CATEGORY_ICONS } from '../../utils/assets';
import { mapChessComResult, ChessComGameItem } from '../../services/ChessComApi/chessComService';

describe('Android <-> Web 1:1 Synchronization Verification', () => {
  it('verifies exact Lichess logistic win probability and single-move accuracy curve', () => {
    expect(WIN_PROB_LOGISTIC_COEFFICIENT).toBe(-0.00368208);
    expect(cpToWhiteWinProbability(0, undefined)).toBeCloseTo(50.0, 5);
    expect(cpToWhiteWinProbability(undefined, 1)).toBeCloseTo(100.0, 5);
    expect(cpToWhiteWinProbability(undefined, 3)).toBeCloseTo(99.5, 5);
    expect(cpToWhiteWinProbability(undefined, -2)).toBeCloseTo(0.25, 5);

    expect(calculateSingleMoveAccuracy(0)).toBeCloseTo(100.0, 5);
    // At winProbLoss = 10: 103.1668100711649 * exp(-0.04354415386753951 * 10) - 3.166924740191411 + 1 = 64.59
    expect(calculateSingleMoveAccuracy(10)).toBeCloseTo(64.6, 1);
  });

  it('verifies all 11 move categories and piece/badge assets match between Web and Android', () => {
    const categories = Object.keys(CATEGORY_ICONS);
    expect(categories).toEqual([
      'Brilliant',
      'Great',
      'Best',
      'Excellent',
      'Good',
      'Book',
      'Forced',
      'Inaccuracy',
      'Mistake',
      'Blunder',
      'MissedChance',
    ]);

    const pieceKeys = Object.keys(PIECE_ASSETS);
    expect(pieceKeys).toEqual([
      'wP', 'wN', 'wB', 'wR', 'wQ', 'wK',
      'bP', 'bN', 'bB', 'bR', 'bQ', 'bK',
    ]);
  });

  it('verifies ECO OpeningBook has >= 42 openings and detects mainline book moves', () => {
    expect(openingBook.getEntriesCount()).toBeGreaterThanOrEqual(42);
    const isE4Book = openingBook.isBookMove(
      'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
      'e4',
      'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1'
    );
    expect(isE4Book).toBe(true);
  });

  it('verifies Chess.com result mapping and CPL threshold classification parity', () => {
    const sampleGame: ChessComGameItem = {
      url: 'https://www.chess.com/game/live/100',
      pgn: '1. e4 e5',
      time_control: '180',
      end_time: 1700000100,
      rated: true,
      time_class: 'blitz',
      rules: 'chess',
      white: { username: 'Hikaru', rating: 3200, result: 'win' },
      black: { username: 'Opponent', rating: 2900, result: 'checkmated' },
    };
    expect(mapChessComResult(sampleGame, 'Hikaru').outcome).toBe('win');
    expect(mapChessComResult(sampleGame, 'Opponent').outcome).toBe('loss');

    const drawGame: ChessComGameItem = {
      ...sampleGame,
      white: { username: 'Hikaru', rating: 3200, result: 'repetition' },
      black: { username: 'Opponent', rating: 2900, result: 'repetition' },
    };
    expect(mapChessComResult(drawGame, 'Hikaru').outcome).toBe('draw');

    const excellentCheck = classifyMoveFromEvaluations({
      evalBeforeCp: 40,
      evalAfterCp: 10,
      player: 'w',
      uci: 'd2d3',
      bestMoveUci: 'd2d4',
      fenBefore: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
      fenAfter: 'rnbqkbnr/pppppppp/8/8/8/3P4/PPP1PPPP/RNBQKBNR b KQkq - 0 1',
      isTheoretical: false,
      thresholds: DEFAULT_THRESHOLDS,
    });
    expect(excellentCheck.category).toBe('Excellent');
  });
});
