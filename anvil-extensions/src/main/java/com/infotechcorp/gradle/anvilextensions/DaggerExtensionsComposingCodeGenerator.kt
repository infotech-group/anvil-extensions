@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions

import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.reference.ClassReference
import org.jetbrains.kotlin.name.FqName

abstract class DaggerExtensionsComposingCodeGenerator : DaggerExtensionsModuleCodeGenerator<Sequence<Sequence<Any?>>>() {

  protected abstract val generators: Sequence<DaggerExtensionsModuleCodeGenerator<Any?>>

  override val classAnnotations: List<FqName> get() = generators
    .flatMapTo(mutableListOf(), DaggerExtensionsModuleCodeGenerator<*>::classAnnotations)

  override fun annotatedClassToContributions(annotatedClass: ClassReference): Sequence<AnvilScopedDaggerContribution<Sequence<Sequence<Any?>>>> {
    val generatorsContributions = generators.map { g -> g.annotatedClassToContributions(annotatedClass) }

    val scopes = generatorsContributions.flatten()
      .mapTo(hashSetOf(), AnvilScopedDaggerContribution<out Any?>::scope)
      .asSequence()

    return scopes.map { scope ->
      AnvilScopedDaggerContribution(
        scope,
        generatorsContributions.map { generatorContribution ->
          generatorContribution
            .filter { contribution -> contribution.scope == scope }
            .map(AnvilScopedDaggerContribution<*>::contribution)
        }
      )
    }
  }

  override fun contributionsToDaggerBindings(contributions: Sequence<Sequence<Sequence<Any?>>>): DaggerBindings {
    val bindings = contributions.flatMap { contribution ->
      contribution.zip(generators).map { (contributions, generator) ->
        generator.contributionsToDaggerBindings(contributions)
      }
    }

    return bindings
      .map { (imports, functions) -> imports to functions }
      .unzip()
      .let { (left, right) -> DaggerBindings(left.asSequence().flatten(), right.asSequence().flatten()) }
  }
}