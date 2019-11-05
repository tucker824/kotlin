package custom.scriptDefinition

import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.*
import java.io.File
import kotlin.script.experimental.location.*

var counter = java.util.concurrent.atomic.AtomicInteger()

class TestDependenciesResolver : AsyncDependenciesResolver {
    suspend override fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return ScriptDependencies(
            classpath = listOf(environment["template-classes"] as File),
            imports = listOf("x" + counter.incrementAndGet())
        ).asSuccess()
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template : Base()

open class Base {
    val i = 3
    val str = ""
}