package net.sharplab.tsuji.tmx.model

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class TranslationUnitVariant @JsonCreator constructor(
        @get:JacksonXmlProperty(isAttribute = true, localName= "lang")
        @param:JacksonXmlProperty(isAttribute = true, localName= "lang")
        val lang: String,
        @get:JacksonXmlProperty(isAttribute = false, localName = "seg")
        @param:JacksonXmlProperty(isAttribute = false, localName = "seg")
        val seg: String) {
}