package net.sharplab.tsuji.tmx

import tools.jackson.databind.DeserializationFeature
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule
import net.sharplab.tsuji.tmx.model.Tmx
import java.nio.file.Path


class TmxCodec {

    private val mapper = XmlMapper.builder()
        .defaultUseWrapper(false)
        .addModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    fun load(path: Path): Tmx {
        return mapper.readValue(path.toFile(), Tmx::class.java)
    }

    fun save(tmx: Tmx, path: Path) {
        mapper.writeValue(path.toFile(), tmx)
    }
}