package com.nikanokh.chessinsightanalyzermobile.domain.chess

import com.nikanokh.chessinsightanalyzermobile.domain.model.OpeningInfo

data class SampleMasterGame(
    val title: String,
    val subtitle: String,
    val pgn: String
)

object EcoOpeningBook {
    private val openings: List<OpeningInfo> = listOf(
        OpeningInfo("C41", "Защита Филидора", "Вариант 3...Bg4 (Опера в Париже)", "e4 e5 Nf3 d6 d4 Bg4"),
        OpeningInfo("C41", "Защита Филидора", "Разменный вариант", "e4 e5 Nf3 d6 d4 exd4"),
        OpeningInfo("C41", "Защита Филидора", "Основная линия", "e4 e5 Nf3 d6"),
        OpeningInfo("C50", "Итальянская партия", "Тихая игра (Giuoco Piano)", "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d3"),
        OpeningInfo("C54", "Итальянская партия", "Главный вариант с c3 и d4", "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d4"),
        OpeningInfo("C51", "Гамбит Эванса", "Принятый гамбит Эванса", "e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bxb4"),
        OpeningInfo("C50", "Итальянская партия", "Основная линия", "e4 e5 Nf3 Nc6 Bc4"),
        OpeningInfo("C65", "Испанская партия", "Берлинская защита", "e4 e5 Nf3 Nc6 Bb5 Nf6"),
        OpeningInfo("C88", "Испанская партия", "Закрытый вариант Чигорина", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3"),
        OpeningInfo("C60", "Испанская партия (Руй Лопес)", "Основная линия", "e4 e5 Nf3 Nc6 Bb5"),
        OpeningInfo("C42", "Русская партия (Защита Петрова)", "Классический вариант", "e4 e5 Nf3 Nf6"),
        OpeningInfo("C44", "Шотландская партия", "Основная линия", "e4 e5 Nf3 Nc6 d4 exd4 Nxd4"),
        OpeningInfo("C33", "Королевский гамбит", "Гамбит слона (Бессмертная партия)", "e4 e5 f4 exf4 Bc4"),
        OpeningInfo("C30", "Королевский гамбит", "Основная линия", "e4 e5 f4"),
        OpeningInfo("B90", "Сицилианская защита", "Вариант Найдорфа", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6"),
        OpeningInfo("B70", "Сицилианская защита", "Вариант Дракона", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6"),
        OpeningInfo("B33", "Сицилианская защита", "Челябинский вариант (Свешникова)", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5"),
        OpeningInfo("B20", "Сицилианская защита", "Основная линия", "e4 c5"),
        OpeningInfo("C11", "Французская защита", "Классический вариант", "e4 e6 d4 d5 Nc3 Nf6"),
        OpeningInfo("C02", "Французская защита", "Вариант Нимцовича (3.e5)", "e4 e6 d4 d5 e5 c5"),
        OpeningInfo("C00", "Французская защита", "Основная линия", "e4 e6"),
        OpeningInfo("B12", "Защита Каро — Канн", "Закрытый вариант (3.e5)", "e4 c6 d4 d5 e5 Bf5"),
        OpeningInfo("B18", "Защита Каро — Канн", "Классический вариант (4...Bf5)", "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5"),
        OpeningInfo("B10", "Защита Каро — Канн", "Основная линия", "e4 c6"),
        OpeningInfo("B07", "Защита Пирца — Уфимцева", "Классическая система", "e4 d6 d4 Nf6 Nc3 g6"),
        OpeningInfo("B01", "Скандинавская защита", "Основная линия", "e4 d5 exd5 Qxd5"),
        OpeningInfo("D37", "Ферзевый гамбит", "Отказанный ферзевый гамбит", "d4 d5 c4 e6 Nc3 Nf6"),
        OpeningInfo("D10", "Славянская защита", "Основная линия", "d4 d5 c4 c6"),
        OpeningInfo("D02", "Лондонская система", "Классическая расстановка с Bf4", "d4 d5 Nf3 Nf6 Bf4"),
        OpeningInfo("E60", "Староиндийская защита", "Классический вариант", "d4 Nf6 c4 g6 Nc3 Bg7"),
        OpeningInfo("D97", "Защита Грюнфельда", "Русская система (Партия века)", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Qb3"),
        OpeningInfo("D80", "Защита Грюнфельда", "Основная линия", "d4 Nf6 c4 g6 Nc3 d5"),
        OpeningInfo("E20", "Защита Нимцовича", "Основная линия", "d4 Nf6 c4 e6 Nc3 Bb4"),
        OpeningInfo("A10", "Английское начало", "Основная линия", "c4"),
        OpeningInfo("A04", "Дебют Рети", "Основная линия", "Nf3 d5 c4"),
        OpeningInfo("C20", "Открытый дебют", "Ход королевской пешки 1.e4 e5", "e4 e5"),
        OpeningInfo("B00", "Дебют королевской пешки", "1.e4", "e4"),
        OpeningInfo("D00", "Дебют ферзевой пешки", "1.d4", "d4")
    ).sortedByDescending { it.movesPrefix.length }

    fun detectOpening(sanMoves: List<String>, fallbackEco: String? = null): OpeningInfo {
        if (sanMoves.isEmpty()) {
            return OpeningInfo(
                eco = fallbackEco ?: "A00",
                name = "Начальная позиция",
                variation = "Стандартная расстановка"
            )
        }
        val cleanMoves = sanMoves.map { it.replace(Regex("[+#?!]+$"), "") }
        val moveLine = cleanMoves.joinToString(" ")

        for (entry in openings) {
            if (moveLine.startsWith(entry.movesPrefix)) {
                return entry
            }
        }
        return OpeningInfo(
            eco = fallbackEco ?: "A00",
            name = "Нестандартное начало",
            variation = cleanMoves.take(4).joinToString(" ")
        )
    }

    val sampleMasterGames: List<SampleMasterGame> = listOf(
        SampleMasterGame(
            title = "Paul Morphy vs Duke Karl / Count Isouard (1-0)",
            subtitle = "C41 · Защита Филидора: Вариант 3...Bg4 (Опера в Париже), 1858",
            pgn = """
                [Event "Опера в Париже"]
                [Site "Paris FRA"]
                [Date "1858.11.02"]
                [White "Paul Morphy"]
                [Black "Duke Karl / Count Isouard"]
                [Result "1-0"]
                [ECO "C41"]

                1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0
            """.trimIndent()
        ),
        SampleMasterGame(
            title = "Garry Kasparov vs Veselin Topalov (1-0)",
            subtitle = "B07 · Защита Пирца — Уфимцева (Бессмертная партия Каспарова), Вейк-ан-Зее 1999",
            pgn = """
                [Event "Hoogovens Wijk aan Zee"]
                [Site "Wijk aan Zee NED"]
                [Date "1999.01.20"]
                [White "Garry Kasparov"]
                [Black "Veselin Topalov"]
                [WhiteElo "2812"]
                [BlackElo "2700"]
                [Result "1-0"]
                [ECO "B07"]

                1. e4 d6 2. d4 Nf6 3. Nc3 g6 4. Be3 Bg7 5. Qd2 c6 6. f3 b5 7. Nge2 Nbd7 8. Bh6 Bxh6 9. Qxh6 Bb7 10. a3 e5 11. O-O-O Qe7 12. Kb1 a6 13. Nc1 O-O-O 14. Nb3 exd4 15. Rxd4 c5 16. Rd1 Nb6 17. g3 Kb8 18. Na5 Ba8 19. Bh3 d5 20. Qf4+ Ka7 21. Rhe1 d4 22. Nd5 Nbxd5 23. exd5 Qd6 24. Rxd4 cxd4 25. Re7+ Kb6 26. Qxd4+ Kxa5 27. b4+ Ka4 28. Qc3 Qxd5 29. Ra7 Bb7 30. Rxb7 Qc4 31. Qxf6 Kxa3 32. Qxa6+ Kxb4 33. c3+ Kxc3 34. Qa1+ Kd2 35. Qb2+ Kd1 36. Bf1 Rd2 37. Rd7 Rxd7 38. Bxc4 bxc4 39. Qxh8 Rd3 40. Qa8 c3 41. Qa4+ Ke1 42. f4 f5 43. Kc1 Rd2 44. Qa7 1-0
            """.trimIndent()
        ),
        SampleMasterGame(
            title = "Donald Byrne vs Bobby Fischer (0-1)",
            subtitle = "D97 · Защита Грюнфельда: Партия века, Нью-Йорк 1956",
            pgn = """
                [Event "Rosenwald Memorial"]
                [Site "New York, NY USA"]
                [Date "1956.10.17"]
                [White "Donald Byrne"]
                [Black "Bobby Fischer"]
                [Result "0-1"]
                [ECO "D97"]

                1. Nf3 Nf6 2. c4 g6 3. Nc3 Bg7 4. d4 O-O 5. Bf4 d5 6. Qb3 dxc4 7. Qxc4 c6 8. e4 Nbd7 9. Rd1 Nb6 10. Qc5 Bg4 11. Bg5 Na4 12. Qa3 Nxc3 13. bxc3 Nxe4 14. Bxe7 Qb6 15. Bc4 Nxc3 16. Bc5 Rfe8+ 17. Kf1 Be6 18. Bxb6 Bxc4+ 19. Kg1 Ne2+ 20. Kf1 Nxd4+ 21. Kg1 Ne2+ 22. Kf1 Nc3+ 23. Kg1 axb6 24. Qb4 Ra4 25. Qxb6 Nxd1 26. h3 Rxa2 27. Kh2 Nxf2 28. Re1 Rxe1 29.Qd8+ Bf8 30. Nxe1 Bd5 31. Nf3 Ne4 32. Qb8 b5 33. h4 h5 34. Ne5 Kg7 35. Kg1 Bc5+ 36. Kf1 Ng3+ 37. Ke1 Bb4+ 38. Kd1 Bb3+ 39. Kc1 Ne2+ 40. Kb1 Nc3+ 41. Kc1 Rc2# 0-1
            """.trimIndent()
        ),
        SampleMasterGame(
            title = "Adolf Anderssen vs Lionel Kieseritzky (1-0)",
            subtitle = "C33 · Королевский гамбит: Бессмертная партия, Лондон 1851",
            pgn = """
                [Event "Immortal Game"]
                [Site "London ENG"]
                [Date "1851.06.21"]
                [White "Adolf Anderssen"]
                [Black "Lionel Kieseritzky"]
                [Result "1-0"]
                [ECO "C33"]

                1. e4 e5 2. f4 exf4 3. Bc4 Qh4+ 4. Kf1 b5 5. Bxb5 Nf6 6. Nf3 Qh6 7. d3 Nh5 8. Nh4 Qg5 9. Nf5 c6 10. g4 Nf6 11. Rg1 cxb5 12. h4 Qg6 13. h5 Qg5 14. Qf3 Ng8 15. Bxf4 Qf6 16. Nc3 Bc5 17. Nd5 Qxb2 18. Bd6 Bxg1 19. e5 Qxa1+ 20. Ke2 Na6 21. Nxg7+ Kd8 22. Qf6+ Nxf6 23. Be7# 1-0
            """.trimIndent()
        )
    )
}
