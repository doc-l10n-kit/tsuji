package net.sharplab.tsuji.core.model.po

class PoMessageIndex(po: Po) {

    private val index : MutableMap<String, PoMessage> = HashMap()

    init {
        po.messages.filter { it.messageString.isNotEmpty() }.forEach{
            index[it.messageId] = it
        }
    }

    operator fun get(key: String): PoMessage?{
        return index[key]
    }

    fun isEmpty() : Boolean{
        return index.isEmpty()
    }
}