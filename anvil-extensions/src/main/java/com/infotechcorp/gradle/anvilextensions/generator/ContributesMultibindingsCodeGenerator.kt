@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions.generator

import com.infotechcorp.gradle.anvilextensions.*
import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.reference.AnnotationReference
import com.squareup.anvil.compiler.internal.reference.AnvilCompilationExceptionAnnotationReference
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.asClassName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

class ContributesMultibindingsCodeGenerator : DaggerExtensionsModuleCodeGenerator<ContributesMultibindingsCodeGenerator.Provision>() {

  data class Provision(
    val clazz: ClassReference,
    val boundType: ClassReference?,
    val boundTypeFromSupertypes: KtSuperTypeListEntry?,
    val mapKey: AnnotationReference.Psi?,
    val qualifier: AnnotationReference.Psi?,
  )

  override val classAnnotations: List<FqName> = listOf(contributesMultibindingsFqName)

  override fun annotatedClassToContributions(annotatedClass: ClassReference): Sequence<AnvilScopedDaggerContribution<Provision>> {
    val annotations = annotatedClass.annotations.filterIsInstance<AnnotationReference.Psi>()

    return annotations.asSequence().withIndex().mapNotNull { (i, annotation) ->
      
      if (annotation.fqName != contributesMultibindingsFqName) return@mapNotNull null

      val mapKey = annotations.getOrNull(i + 1)?.takeIf(AnnotationReference::isMapKey)
      val boundType = annotation.boundTypeOrNull()
      val boundTypeFromSupertype = annotation.boundTypeIndex()
        .run((annotatedClass as ClassReference.Psi).clazz.superTypeListEntries::getOrNull)

      val qualifier = annotatedClass.annotations.find(AnnotationReference::isQualifier)
        ?.takeUnless { annotation.ignoreQualifier() }

      if (boundType == null && boundTypeFromSupertype == null) {
        throw AnvilCompilationExceptionAnnotationReference(annotation,
          "Either boundType or boundTypeIndex must be specified")
      }

      AnvilScopedDaggerContribution(
        scope = annotation.scope(),
        contribution = Provision(
          clazz = annotatedClass,
          boundType = boundType,
          boundTypeFromSupertypes = boundTypeFromSupertype,
          mapKey = mapKey,
          qualifier = qualifier,
        )
      )
    }
  }

  override fun contributionsToDaggerBindings(contributions: Sequence<Provision>): DaggerBindings {
    val imports = sequence {
      for ((clazz, boundType, boundTypeFromSupertypes, mapKey, qualifier) in contributions) {
        yield(bindsFqName)
        if (mapKey != null) yield(intoMapFqName)
        if (mapKey == null) yield(intoSetFqName)
        if (mapKey != null) yield(mapKey.fqName)
        if (qualifier != null) yield(qualifier.fqName)
        if (boundType != null) yield(boundType.fqName)

        yield(clazz.fqName)
        yieldAll(boundTypeFromSupertypes?.run { containingKtFile.pullAllImportsRelatedToType(this) }.orEmpty())
        yieldAll(mapKey?.pullAllImports().orEmpty())
        yieldAll(qualifier?.pullAllImports().orEmpty())
      }
    }

    val functions = contributions.map { (clazz, boundType, boundTypeFromSupertypes, mapKey, qualifier) ->
      val annotations = sequence {
        yield("@Binds")
        if (mapKey == null) yield("@IntoSet")
        if (mapKey != null) yield("@IntoMap")
        if (mapKey != null) yield(mapKey.annotation.text)
        if (qualifier != null) yield(qualifier.annotation.text)
      }

      val boundTypeName = if (boundType != null) {
        var boundTypeName = boundType.asClassName().toString()
        if (boundType.isGenericClass()) {
          boundTypeName += boundType.typeParameters.joinToString(",", "<", ">") { "*" }
        }
        boundTypeName
      } else {
        boundTypeFromSupertypes!!.text
      }

      DaggerBindings.Function(annotations, "(a: ${clazz.shortName}): $boundTypeName")
    }

    return DaggerBindings(imports, functions)
  }
}
