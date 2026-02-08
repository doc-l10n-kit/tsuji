package net.sharplab.tsuji.tmx.model

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class TranslationUnit @JsonCreator constructor(
        @get:JacksonXmlElementWrapper(useWrapping = false)
        @param:JacksonXmlElementWrapper(useWrapping = false)
        @get:JacksonXmlProperty(isAttribute = false, localName = "tuv")
        @param:JacksonXmlProperty(isAttribute = false, localName = "tuv")
        val variants: List<TranslationUnitVariant>) {
}