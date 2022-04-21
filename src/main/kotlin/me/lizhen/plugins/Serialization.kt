package me.lizhen.plugins


import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import java.io.File

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

class tt {

    fun replaceFile(f: File, from: String, to: String) {
        val newContent = f.readLines().map {
            it.replace(Regex(from), to)
        }.joinToString("\n")
        f.writeText(newContent)
    }

    val requiredFiles = listOf(
        "cpp.js",
        "vendor.js",
        "index.js"
    )

    fun a() {
        File("./dist/assets").listFiles()?.forEach {
            println("Processing ${it.name}")
            val nn = it.name.replace(
                Regex("""\.[0-9a-z]+\."""),
                "."
            )
            if (nn.endsWith(".js") && !requiredFiles.contains(nn)) return@forEach
            val newFile = File("../extension/pages/assets/${nn}")
            it.copyTo(newFile, true)

            if (nn.startsWith("vendor")) {
                replaceFile(newFile, """cpp\.[0-9a-z]+\.js""", "cpp.js")
            } else if (nn.startsWith("index")) {
                replaceFile(newFile, """vendor\.[0-9a-z]+\.js""", "vendor.js")
            }
        }

        File("./package.json").readLines().map {
            if (it.startsWith("    \"version\": ")) {
                val v = it.replace("    \"version\": \"", "").replace("\",\n", "")
                val vSep = v.split(("."))
                val newV = vSep.last().toInt().inc()
                return@map "    \"version\": \"0.0.$newV\",\n"
            }
            it
        }

        val codiconStyleCssString = Regex("""\t@font-face \{\n\t\tfont-family: "codicon";\n\t\t.+\n\t\t.+\n\t}""")

    }

}
