fun func2(): Int {
    if (true) {
        print("foo")
    } else {
        if (false)
            return 0
    }
    println("hi")
    return 42
}