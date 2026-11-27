package com.nokia.launcher

import com.nokia.launcher.ui.tones.TUNES
import com.nokia.launcher.ui.tones.render
import com.nokia.launcher.ui.tones.wav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TunesTest {
    @Test
    fun everyTuneRendersAsAValidWav() {
        assertEquals(TUNES.size, TUNES.map { it.id }.toSet().size)
        TUNES.forEach { tune ->
            val samples = render(tune)
            val file = wav(samples)
            assertTrue(tune.id, samples.any { it.toInt() != 0 })
            assertEquals("RIFF", String(file, 0, 4))
            assertEquals(samples.size * 2, ByteBuffer.wrap(file, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int)
            assertEquals(44 + samples.size * 2, file.size)
        }
    }
}
