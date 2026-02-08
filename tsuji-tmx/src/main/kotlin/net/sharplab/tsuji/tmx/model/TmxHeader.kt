package net.sharplab.tsuji.tmx.model

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class TmxHeader @JsonCreator constructor(
        @get:JacksonXmlProperty(isAttribute = true, localName="creationtool")
        @param:JacksonXmlProperty(isAttribute = true, localName="creationtool")
        val creationTool: String,

        @get:JacksonXmlProperty(isAttribute = true, localName="creationtoolversion")
        @param:JacksonXmlProperty(isAttribute = true, localName="creationtoolversion")
        val creationToolVersion: String,

        @get:JacksonXmlProperty(isAttribute = true, localName="segtype")
        @param:JacksonXmlProperty(isAttribute = true, localName="segtype")
        val segType: String,

        @get:JacksonXmlProperty(isAttribute = true, localName="o-tmf")
        @param:JacksonXmlProperty(isAttribute = true, localName="o-tmf")
        val oTmf: String,

        @get:JacksonXmlProperty(isAttribute = true, localName="adminlang")
        @param:JacksonXmlProperty(isAttribute = true, localName="adminlang")
        val adminLang: String,

        @get:JacksonXmlProperty(isAttribute = true, localName="srclang")
        @param:JacksonXmlProperty(isAttribute = true, localName="srclang")
        val srcLang: String,

        @get:JacksonXmlProperty(isAttribute = true, localName="datatype")
        @param:JacksonXmlProperty(isAttribute = true, localName="datatype")
        val dataType: String
)