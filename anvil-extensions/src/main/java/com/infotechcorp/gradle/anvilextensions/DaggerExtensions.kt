@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions

import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.fqName
import com.squareup.anvil.compiler.internal.reference.AnnotationReference
import com.squareup.anvil.compiler.internal.reference.argumentAt
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

val contributesProvidersFqName = ContributesProviders::class.fqName
val consumesMultibindingFqName = ConsumesMultibinding::class.fqName
val contributesMultibindingsFqName = ContributesMultibindings::class.fqName

val providerFqName        = FqName("javax.inject.Provider")
val injectFqName          = FqName("javax.inject.Inject")
val providesFqName        = FqName("dagger.Provides")
val bindsFqName           = FqName("dagger.Binds")
val lazyFqName            = FqName("dagger.Lazy")
val intoSetFqName         = FqName("dagger.multibindings.IntoSet")
val intoMapFqName         = FqName("dagger.multibindings.IntoMap")
val elementsIntoSetFqName = FqName("dagger.multibindings.ElementsIntoSet")
val multibindsFqName      = FqName("dagger.multibindings.Multibinds")
val assistedInjectFqName  = FqName("dagger.assisted.AssistedInject")
val assistedFqName        = FqName("dagger.assisted.Assisted")
val bindsOptionalOfFqName = FqName("dagger.BindsOptionalOf")
val optionalFqName        = FqName("java.util.Optional")

fun AnnotationReference.ignoreQualifier(): Boolean {
  return argumentAt("ignoreQualifier", 3)?.value() ?: false
}

fun AnnotationReference.boundTypeIndex(): Int {
  return argumentAt("boundTypeIndex", 2)?.value() ?: -1
}

fun String.injectedTypeToKotlinSourceSanitizedForDagger(): String {
  return this
    .removeGenericLayer(providerFqName)
    .removeGenericLayer(lazyFqName)
    .replace("kotlin.collections.", "")//Set/Map
    .replace("kotlin.jvm.", "")//JvmSuppressWildcard
    .replace("kotlin.", "")//cleanup primitives
    .replace("@JvmSuppressWildcards @JvmSuppressWildcards", "@JvmSuppressWildcards")
    .removePrefix("@JvmSuppressWildcards ")
}

fun String.removeGenericLayer(layer: FqName): String {
  val name = layer.asString()
  return if (contains(name)) {
    replace("$name<", "").removeSuffix(">")
  } else this
}

fun KtFile.pullAllImportsRelatedToType(type: PsiElement, isAllUnder: Boolean = true): Sequence<FqName> {
  val text = type.text
  return importList?.imports.orEmpty().asSequence().filter { import ->
    val importPath = import.importPath
    (isAllUnder && import.isAllUnder) ||
      (importPath != null && importPath.fqName.shortName().asString() in text)
  }.mapNotNull { import -> import.importPath?.pathStr?.let(::FqName) }
}

fun AnnotationReference.Psi.pullAllImports(): Sequence<FqName> {
  val text = annotation.text
  if ('\"' in text || "()" in text || text[text.lastIndex - 1].isDigit()) return emptySequence()

  return annotation.containingKtFile.pullAllImportsRelatedToType(annotation)
}