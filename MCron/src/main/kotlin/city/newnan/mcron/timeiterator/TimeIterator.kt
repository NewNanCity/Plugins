package city.newnan.mcron.timeiterator

interface TimeIterator {
    fun getNextTime(now: Long): Long
    fun onExecute()
}