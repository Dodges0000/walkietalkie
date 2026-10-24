package com.example.walkietalkie.voice;

import java.util.Random;

/**
 * Эффект рации: полосовой фильтр, сильный перегруз (клиппинг), "грубое" квантование,
 * даунсэмплинг и тихое отфильтрованное шипение.
 * У каждого говорящего свой экземпляр (фильтры хранят состояние).
 */
public final class RadioEffect {
    private static final double FS = 48000.0;

    // ===================== НАСТРОЙКИ (крути на слух) =====================
    /** Предусиление перед клиппингом. Больше = сильнее хрипы и перегруз. 3..15 */
    private static final double DRIVE = 9.0;
    /** Порог жёсткого клиппинга (0.2..1.0). Меньше = грубее, "плющит" звук. */
    private static final double CLIP = 0.45;
    /** Глубина квантования в битах. Меньше = более "цифровой" хрип. 5..10 (0 = выкл) */
    private static final int BITS = 7;
    /** Во сколько раз занижаем частоту дискретизации (1 = выкл, 3 = 16 кГц). */
    private static final int DOWNSAMPLE = 3;
    /** Громкость шипения. 0 = без шума. Было 0.015 (слишком громко). */
    private static final double NOISE = 0.004;
    /** Итоговая громкость. */
    private static final double OUT_GAIN = 0.85;
    // =====================================================================

    // полоса голоса 400..2800 Гц
    private final Biquad hp1 = Biquad.highPass(FS, 400, 0.707);
    private final Biquad hp2 = Biquad.highPass(FS, 400, 0.707);
    private final Biquad lp1 = Biquad.lowPass(FS, 2800, 0.707);
    private final Biquad lp2 = Biquad.lowPass(FS, 2800, 0.707);

    // сглаживание после перегруза, чтобы не было неприятного "колючего" звона
    private final Biquad post = Biquad.lowPass(FS, 3200, 0.707);

    // шум тоже пропускаем через полосу, чтобы он не был резким широкополосным
    private final Biquad noiseHp = Biquad.highPass(FS, 500, 0.707);
    private final Biquad noiseLp = Biquad.lowPass(FS, 3000, 0.707);

    private final Random random = new Random();

    private double held;
    private int holdCounter;

    /** Обрабатывает PCM (16 бит, моно, 48 кГц) на месте. */
    public void apply(short[] pcm) {
        final double levels = BITS > 0 ? (1 << (BITS - 1)) : 0;

        for (int i = 0; i < pcm.length; i++) {
            double x = pcm[i] / 32768.0;

            // 1. полоса пропускания рации
            x = hp2.process(hp1.process(x));
            x = lp2.process(lp1.process(x));

            // 2. сильный перегруз: усиливаем и жёстко обрезаем
            x *= DRIVE;
            x = Math.tanh(x);              // скруглённый перегруз
            x = Math.max(-CLIP, Math.min(CLIP, x)); // жёсткий срез
            x /= CLIP;                     // возвращаем уровень к ~1.0

            // 3. квантование (цифровая "каша")
            if (levels > 0) x = Math.round(x * levels) / levels;

            // 4. даунсэмплинг (ступеньки)
            if (DOWNSAMPLE > 1) {
                if (holdCounter++ % DOWNSAMPLE == 0) held = x;
                x = held;
            }

            // 5. сглаживание
            x = post.process(x);

            // 6. тихое шипение в полосе рации
            if (NOISE > 0) {
                double n = noiseLp.process(noiseHp.process(random.nextDouble() * 2 - 1));
                x += n * NOISE * 4; // фильтры срезают энергию, компенсируем множителем
            }

            x *= OUT_GAIN;
            pcm[i] = (short) Math.max(-32768, Math.min(32767, Math.round(x * 32767)));
        }
    }
}
