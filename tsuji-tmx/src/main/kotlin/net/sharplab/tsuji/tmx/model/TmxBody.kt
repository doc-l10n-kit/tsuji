package net.sharplab.tsuji.tmx.model

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class TmxBody @JsonCreator constructor(

        @get:JacksonXmlElementWrapper(useWrapping = false)

        @param:JacksonXmlElementWrapper(useWrapping = false)

        @get:JacksonXmlProperty(isAttribute = false, localName = "tu")

        @param:JacksonXmlProperty(isAttribute = false, localName = "tu")

        val translationUnits: List<TranslationUnit>?

)
