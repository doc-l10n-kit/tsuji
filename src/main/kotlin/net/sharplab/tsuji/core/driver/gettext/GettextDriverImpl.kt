package net.sharplab.tsuji.core.driver.gettext

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class GettextDriverImpl(
    private val externalProcessDriver: ExternalProcessDriver
) : GettextDriver {

    override fun normalize(path: Path) {
        // msgcat normalizes the format (line breaks, encoding, etc.)
        // Note: obsolete entries and POT-Creation-Date are already removed by PoDriver.load/save in PoNormalizerService
        externalProcessDriver.execute(
            command = listOf("msgcat", "--to-code=utf-8", "--no-wrap", "-o", path.toString(), path.toString()),
            timeoutValue = 60,
            timeoutUnit = TimeUnit.SECONDS
        )
    }
}
