import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

class ConfigReader private constructor() {

    private var inputStream: InputStream? = null
    private val prop = Properties()

    init {
        try {
            val propFileName = "config.properties"
            inputStream = javaClass.classLoader.getResourceAsStream(propFileName)

            if (inputStream != null) {
                prop.load(inputStream)
            } else {
                throw FileNotFoundException("property file '$propFileName' not found in the classpath")
            }
        } catch (e: Exception) {
            println("Exception: $e")
        } finally {
            inputStream!!.close()
        }
    }

    fun getStringProperty(name: String): String = prop.getProperty(name)

    private object Holder { val INSTANCE = ConfigReader() }

    companion object {
        val instance: ConfigReader by lazy { Holder.INSTANCE }
    }
}