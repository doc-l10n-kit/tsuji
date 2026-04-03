package net.sharplab.tsuji.app.exception

class TsujiAppException : RuntimeException {
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(message: String?) : super(message)
}