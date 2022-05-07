@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions.generator

import com.infotechcorp.gradle.anvilextensions.*
import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.reference.AnnotationReference
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.FunctionReference
import com.squareup.anvil.compiler.internal.reference.Visibility.PUBLIC
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction

class ContributesProvidersCodeGenerator : DaggerExtensionsModuleCodeGenerator<ContributesProvidersCodeGenerator.Provision>() {

  data class Provision(
    val clazz: ClassReference,
    val providerFunction: KtFunction,
    val scope: AnnotationReference.Psi?,
    val multibinding: Boolean,
    val elementsIntoSet: Boolean,
    val mapKey: AnnotationReference.Psi?,
    val qualifier: AnnotationReference.Psi?,
  )

  override val classAnnotations: List<FqName> = listOf(contributesProvidersFqName)

  override fun annotatedClassToContributions(annotatedClass: ClassReference): Sequence<AnvilScopedDaggerContribution<Provision>> {
    val annotation = annotatedClass.annotations.first { ann -> ann.fqName == contributesProvidersFqName }

    val implementsProvider = annotatedClass.directSuperClassReferences().any { s -> s.fqName == providerFqName }

    val providerFunctions = annotatedClass.functions.filter { f ->
      f.visibility() == PUBLIC && f.parameters.isEmpty() &&
        (implementsProvider && f.name == "get") || f.annotations.any { a ->
          a.isDaggerScope() || a.isMapKey() || a.isQualifier() ||
          a.fqName == providesFqName || a.fqName == intoSetFqName || a.fqName == elementsIntoSetFqName
      }
    }

    return providerFunctions.asSequence().map { providerFunction ->
      val annotations = providerFunction.annotations.filterIsInstance<AnnotationReference.Psi>()
      
      val mapKey = annotations.find(AnnotationReference::isMapKey)
      val qualifier = annotations.find(AnnotationReference::isQualifier)
      val daggerScope = annotations.find(AnnotationReference::isDaggerScope)
      val elementsIntoSet = annotations.any { a -> a.fqName == elementsIntoSetFqName }
      
      val anvilScope = annotation.scope()
      val multibinding = mapKey != null || providerFunction.isAnnotatedWith(intoSetFqName)

      AnvilScopedDaggerContribution(
        scope = anvilScope,
        contribution = Provision(
          clazz = annotatedClass,
          providerFunction = (providerFunction as FunctionReference.Psi).function,
          scope = daggerScope,
          multibinding = multibinding,
          elementsIntoSet = elementsIntoSet,
          mapKey = mapKey,
          qualifier = qualifier,
        )
      )
    }
  }

  override fun contributionsToDaggerBindings(contributions: Sequence<Provision>): DaggerBindings {
    val imports = sequence {
      for ((clazz, providerFunction, scope, multibinding, elementsIntoSet, mapKey, qualifier) in contributions) {
        yield(providesFqName)
        if (multibinding && mapKey != null) yield(intoMapFqName)
        if (multibinding && mapKey == null) yield(intoSetFqName)
        if (elementsIntoSet) yield(elementsIntoSetFqName)
        if (scope != null) yield(scope.fqName)
        if (mapKey != null) yield(mapKey.fqName)
        if (qualifier != null) yield(qualifier.fqName)
        yield(clazz.fqName)
        yieldAll(providerFunction.containingKtFile.pullAllImportsRelatedToType(providerFunction.typeReference!!))
        yieldAll(mapKey?.pullAllImports().orEmpty())
        yieldAll(qualifier?.pullAllImports().orEmpty())
      }
    }

    val functions = contributions.map { (clazz, providerFunction, scope, multibinding, elementsIntoSet, mapKey, qualifier) ->
      val annotations = sequence {
        yield("@Provides")

        if (multibinding && mapKey != null) yield("@IntoMap")
        if (multibinding && mapKey == null) yield("@IntoSet")
        if (elementsIntoSet)                yield("@ElementsIntoSet")
        if (scope != null && !multibinding) yield(scope.annotation.text)
        if (mapKey != null)                 yield(mapKey.annotation.text)
        if (qualifier != null)              yield(qualifier.annotation.text)
      }

      val providedType = providerFunction.typeReference?.text?.injectedTypeToKotlinSourceSanitizedForDagger()

      DaggerBindings.Function(annotations, "(a: ${clazz.shortName}): $providedType = a.${providerFunction.name}()")
    }

    return DaggerBindings(imports, functions)
  }

  override fun generateScopedDaggerModuleFromContributions(
    scope: ClassReference, contributions: List<Provision>, pckg: String, moduleName: String
  ): String {
    return super.generateScopedDaggerModuleFromContributions(scope, contributions, pckg, moduleName)
      .replaceFirst("abstract class", "object")
      .replace("abstract fun", "fun")
  }
}
