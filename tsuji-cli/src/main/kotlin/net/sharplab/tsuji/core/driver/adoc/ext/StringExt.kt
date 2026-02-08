package net.sharplab.tsuji.core.driver.adoc.ext

import org.slf4j.LoggerFactory

class StringExt {
companion object{

    private val logger = LoggerFactory.getLogger(StringExt::class.java)

    fun String.indexOfBeginningOfLine(lineNumber: Int): Int{
        val lines = this.lines()
        if(lineNumber < 1 || lineNumber > lines.size){
            return -1
        }
        return lines.subList(0, lineNumber-1).sumOf { it.length + 1 }
    }

    fun String.indexOfEndOfLine(lineNumber: Int): Int{
        val lines = this.lines()
        if(lineNumber < 1 || lineNumber > lines.size){
            return -1
        }
        return lines.subList(0, lineNumber).joinToString("\n").length
    }

    fun String.normalizeLineBreak(): String{
        return this.replace("\r\n", "\n")
    }

    fun String.calculateLines(): Int{
        return this.split("\n").size
    }
}
}
