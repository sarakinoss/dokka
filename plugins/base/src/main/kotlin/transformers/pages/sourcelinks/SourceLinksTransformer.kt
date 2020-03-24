package org.jetbrains.dokka.base.transformers.pages.sourcelinks

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.DescriptorDocumentableSource
import org.jetbrains.dokka.model.WithExpectActual
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.getPsi

class SourceLinksTransformer(val context: DokkaContext) : PageTransformer {

    private lateinit var sourceLinks: List<SourceLink>

    override fun invoke(input: RootPageNode): RootPageNode {

        sourceLinks = context.configuration.passesConfigurations
            .flatMap { it.sourceLinks.map { sl -> SourceLink(sl, it.platformData) } }

        return input.transformContentPagesTree { node ->
            (node.documentable as? WithExpectActual)?.sources?.map?.entries?.fold(node) { acc, entry ->
                sourceLinks.find { entry.value.path.contains(it.path) && it.platformData == entry.key }?.run {
                    acc.modified(
                        content = acc.content.addSource(
                            entry.key,
                            (entry.value as DescriptorDocumentableSource).toLink(this)
                        )
                    )
                } ?: acc
            } ?: node
        }
    }

    private fun DescriptorDocumentableSource.toLink(sourceLink: SourceLink): String =
        sourceLink.url +
        this.path.split(sourceLink.path)[1] +
        sourceLink.lineSuffix +
        "${(this.descriptor as DeclarationDescriptorWithSource).source.getPsi()?.lineNumber()}"

    private fun ContentNode.addSource(platformData: PlatformData, address: String?): ContentNode =
        if (address != null) when (this) {
            is ContentGroup -> copy(
                children = children + listOf(
                    PlatformHintedContent(
                        ContentResolvedLink(
                            children = listOf(
                                ContentText(
                                    text = "(source)",
                                    dci = DCI(dci.dri, ContentKind.BriefComment),
                                    platforms = setOf(platformData),
                                    style = emptySet(),
                                    extra = PropertyContainer.empty()
                                )
                            ),
                            address = address,
                            extra = PropertyContainer.empty(),
                            dci = DCI(dci.dri, ContentKind.Source),
                            platforms = setOf(platformData),
                            style = emptySet()
                        ),
                        setOf(platformData)
                    )
                )
            )
            else -> this
        } else this

    private fun PsiElement.lineNumber(): Int? {
        val doc = PsiDocumentManager.getInstance(project).getDocument(containingFile)
        // IJ uses 0-based line-numbers; external source browsers use 1-based
        return doc?.getLineNumber(textRange.startOffset)?.plus(1)
    }
}

data class SourceLink(val path: String, val url: String, val lineSuffix: String?, val platformData: PlatformData) {
    constructor(sourceLinkDefinition: DokkaConfiguration.SourceLinkDefinition, platformData: PlatformData) : this(
        sourceLinkDefinition.path, sourceLinkDefinition.url, sourceLinkDefinition.lineSuffix, platformData
    )
}