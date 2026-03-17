package com.motofader.audio

fun computeOutput(input: Float, threshold: Float, ratio: Float, kneeWidth: Float): Float {
    val kneeStart = threshold - kneeWidth / 2f
    val kneeEnd = threshold + kneeWidth / 2f

    return when {
        kneeWidth < 0.1f || input < kneeStart -> {
            // Below knee: unity
            if (input < threshold) input
            // Above threshold (hard knee)
            else threshold + (input - threshold) / ratio
        }
        input > kneeEnd -> {
            // Above knee: full compression
            threshold + (input - threshold) / ratio
        }
        else -> {
            // In the knee: quadratic interpolation
            val xRel = input - kneeStart
            input + ((1f / ratio - 1f) * xRel * xRel) / (2f * kneeWidth)
        }
    }
}
