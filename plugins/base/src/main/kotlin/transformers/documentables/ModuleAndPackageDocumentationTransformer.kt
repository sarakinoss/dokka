package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.PlatformDependent
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Files
import java.nio.file.Paths


internal object ModuleAndPackageDocumentationTransformer : PreMergeDocumentableTransformer {

    override fun invoke(original: List<DModule>, context: DokkaContext): List<DModule> {

        val modulesAndPackagesDocumentation =
            context.configuration.passesConfigurations
                .map {
                    Pair(it.moduleName, it.platformData) to
                        it.includes.map { Paths.get(it) }
                            .filter { Files.exists(it) }
                            .flatMap {
                                it.toFile()
                                    .readText()
                                    .split(Regex("(\n|^)# (?=(Module|Package))")) // Matches heading with Module/Package to split by
                                    .filter { it.isNotEmpty() }
                                    .map { it.split(Regex(" "), 2) } // Matches space between Module/Package and fully qualified name
                            }.groupBy({ it[0] }, {
                                it[1].split(Regex("\n"), 2) // Matches new line after fully qualified name
                                    .let { it[0] to it[1].trim() }
                            }).mapValues {
                                it.value.toMap()
                            }
                }.toMap()

        return original.map { module ->

            val moduleDocumentation =
                module.platformData.mapNotNull {
                    val doc = modulesAndPackagesDocumentation[Pair(module.name, it)]
                    val facade = context.platforms[it]!!.facade
                    doc?.get("Module")?.get(module.name)?.run {
                        it to MarkdownParser(
                            facade,
                            facade.moduleDescriptor
                        ).parse(this)
                    }
                }.toMap()

            val packagesDocumentation = module.packages.map {
                it.name to it.platformData.mapNotNull { pd ->
                    val doc = modulesAndPackagesDocumentation[Pair(module.name, pd)]
                    val facade = context.platforms[pd]!!.facade
                    doc?.get("Package")?.get(it.name)?.run {
                        pd to MarkdownParser(
                            facade,
                            facade.resolveSession.getPackageFragment(FqName(it.name))!!
                        ).parse(this)
                    }
                }.toMap()
            }.toMap()

            module.copy(
                documentation = module.documentation.let { PlatformDependent(it.map + moduleDocumentation) },
                packages = module.packages.map {
                    if(packagesDocumentation[it.name] != null)
                        it.copy(documentation = it.documentation.let { value ->
                            PlatformDependent(value.map + packagesDocumentation[it.name]!!)
                        })
                    else
                        it
                }
            )
        }
    }
}
