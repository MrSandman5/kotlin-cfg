fun func3(): Int {
    for (i in 1..10) {
        println(i)
    }
    for (j in arrayListOf(1,2,3).indices)
        println(j)
    for (k in 1..3)
        for (l in 4..6) {
            println("${k}${l}")
        }
    return 42
}