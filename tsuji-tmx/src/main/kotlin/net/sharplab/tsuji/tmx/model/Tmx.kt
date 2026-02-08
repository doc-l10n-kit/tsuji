package net.sharplab.tsuji.tmx.model

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class Tmx @JsonCreator constructor(
        @get:JacksonXmlProperty(isAttribute = true, localName = "version")
        @param:JacksonXmlProperty(isAttribute = true, localName = "version")
        val version: String,
        @get:JacksonXmlProperty(isAttribute = false, localName = "header")
        @param:JacksonXmlProperty(isAttribute = false, localName = "header")
        val tmxHeader: TmxHeader,
        @get:JacksonXmlProperty(isAttribute = false, localName = "body")
        @param:JacksonXmlProperty(isAttribute = false, localName = "body")
        val tmxBody: TmxBody) {
}
