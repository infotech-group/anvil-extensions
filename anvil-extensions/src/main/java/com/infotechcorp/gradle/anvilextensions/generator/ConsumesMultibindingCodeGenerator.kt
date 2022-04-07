@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions.generator

import com.infotechcorp.gradle.anvilextensions.*
import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.reference.AnnotationReference
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.ParameterReference
import com.squareup.anvil.compiler.internal.reference.PropertyReference
import org.jetbrains.kotlin.name.FqName

class ConsumesMultibindingCodeGenerator : DaggerExtensionsModuleCodeGenerator<ConsumesMultibindingCodeGenerator.MultibindingDeclaration>() {

  data class MultibindingDeclaration(
    val typeSource: String,
    val bindsOptional: Boolean,
    val qualifier: AnnotationReference.Psi?
  )

  override val classAnnotations: List<FqName> = listOf(consumesMultibindingFqName)

  override fun annotatedClassToContributions(annotatedClass: ClassReference): Sequence<AnvilScopedDaggerContribution<MultibindingDeclaration>> {
    val annotation = annotatedClass.annotations.find { annotation -> annotation.fqName == consumesMultibindingFqName }
      ?: return emptySequence()

    val constructorProperties = annotatedClass.constructors.firstNotNullOfOrNull { c ->
      when {
        c.isAnnotatedWith(injectFqName)         -> c.parameters
        c.isAnnotatedWith(assistedInjectFqName) -> c.parameters.filter { p -> !p.isAnnotatedWith(assistedFqName) }
        else                                    -> null
      }?.associateWith(ParameterReference::type)
    }.orEmpty()

    val properties = annotatedClass.properties
      .filter { prop -> annotatedClass.isInterface() || prop.isAnnotatedWith(injectFqName) }
      .associateWith(PropertyReference::type)

    val types = constructorProperties + properties

    return types.entries.asSequence().mapNotNull { (annotatedReference, typeReference) ->
      val qualifier = annotatedReference.annotations.find(AnnotationReference::isQualifier) as? AnnotationReference.Psi

      var type = typeReference.asTypeName().toString()

      val bindsOptional = type.contains(optionalFqName.asString())
      val multibinding = type.contains("kotlin.collections")

      if (!multibinding && !bindsOptional) return@mapNotNull null

      if (bindsOptional) {
        type = type.removeGenericLayer(optionalFqName)
      }

      type = type.injectedTypeToKotlinSourceSanitizedForDagger()

      AnvilScopedDaggerContribution(
        scope = annotation.scope(),
        contribution = MultibindingDeclaration(type, bindsOptional, qualifier),
      )
    }
  }

  override fun contributionsToDaggerBindings(contributions: Sequence<MultibindingDeclaration>): DaggerBindings {
    val imports = sequence {
      yield(multibindsFqName)

      for ((_, bindsOptional, qualifier) in contributions) {
        if (bindsOptional) yield(bindsOptionalOfFqName)
        if (qualifier != null) yield(qualifier.fqName)
        yieldAll(qualifier?.pullAllImports().orEmpty())
      }
    }

    val bindFunctions = contributions.map { (typeSource, bindsOptional, qualifier) ->
      val annotations = sequence {
        if (bindsOptional) yield("@BindsOptionalOf")
        if (!bindsOptional) yield("@Multibinds")
        if (qualifier != null) yield(qualifier.annotation.text)
      }

      DaggerBindings.Function(annotations, "(): $typeSource")
    }

    return DaggerBindings(imports, bindFunctions)
  }
}