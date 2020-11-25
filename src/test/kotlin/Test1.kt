fun func1() {
    val a = "constant"
    var b = 0

    while (b < 10) {
        if (b < 5) {
            println("b lt 5")
        } else {
            println("b gt 5")
        }
        b++
    }

    if (true)
        return

    if (false) {
        return
    }

    println(a)
}