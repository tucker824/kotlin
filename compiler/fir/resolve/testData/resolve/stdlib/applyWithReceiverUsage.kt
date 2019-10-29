inline fun <T> T.myApply(block: T.() -> Unit) {
    block()
}

fun test() {
    mutableListOf<String>().apply {
        add("Alpha")
    }

    mutableListOf<String>().myApply {
        add("Omega")
    }
}