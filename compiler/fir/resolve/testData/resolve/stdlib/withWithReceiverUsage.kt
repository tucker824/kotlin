class A {
    fun foo() {}
}

inline fun <T> myWith(receiver: T, block: T.() -> Unit) {
    receiver.block()
}

fun test_1() {
    myWith(A()) {
        foo()
    }

    with(A()) {
        foo()
    }
}