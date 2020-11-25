fun func7(n: Int): Int {
    val a = 0
    val b = 1
    var c = 0
    var i: Int
    if (n < 2) return n
    i = 1
    while (i < n) {
        c = a + b
        if (a < b) {
            i++
            continue
        }
        var j = 1
        while (j < i) {
            val k = b - a
            if (a < k) break
            val y: Int = a + k + 42
            ++j
        }
        val l = b - a
        i++
    }
    return c
}