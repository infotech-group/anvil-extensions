@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions

import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.api.AnvilContext
import com.squareup.anvil.compiler.api.CodeGenerator
import com.squareup.anvil.compiler.api.GeneratedFile
import com.squareup.anvil.compiler.api.createGeneratedFile
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class DaggerExtensionsModuleCodeGenerator<T> : CodeGenerator {

  abstract val classAnnotations: List<FqName>

  abstract fun annotatedClassToContributions(annotatedClass: ClassReference): Sequence<AnvilScopedDaggerContribution<T>>
  abstract fun contributionsToDaggerBindings(contributions: Sequence<T>): DaggerBindings

  class AnvilScopedDaggerContribution<T>(
    val scope: ClassReference,
    val contribution: T
  )

  data class DaggerBindings(
    val imports: Sequence<FqName>,
    val functions: Sequence<Function>,
  ) {
    class Function(
      val annotations: Sequence<String>,
      val body: String,
    )
  }

  override fun generateCode(codeGenDir: File, module: ModuleDescriptor, projectFiles: Collection<KtFile>): Collection<GeneratedFile> {
    val classAnnotations = classAnnotations

    val annotatedClasses = projectFiles
      .classAndInnerClassReferences(module)
      .filter { clazz -> classAnnotations.any(clazz::isAnnotatedWith) }
/* FIXME https://github.com/square/anvil/issues/599

    var pckg = ""

    val contributionsPerModule = annotatedClasses
      .onEach { clazz ->
        val clsPckg = clazz.packageFqName.asString()
        if (pckg.isEmpty() || pckg.length > clsPckg.length) {
          pckg = clsPckg
        }
      }
      .flatMap(::annotatedClassToContributions)
      .groupBy(AnvilScopedDaggerContribution<T>::scope)
*/
    return annotatedClasses.associateWith(::annotatedClassToContributions).mapNotNull { (clazz, contributions) ->
      val scope = contributions.firstOrNull()?.scope ?: return@mapNotNull null
      val moduleName = clazz.shortName + javaClass.simpleName.filter(Char::isUpperCase).dropLast(2) + "Module"
      val pckg = clazz.packageFqName.asString()

      createGeneratedFile(codeGenDir, pckg, moduleName,
        generateScopedDaggerModuleFromContributions(
          scope,
          contributions.map(AnvilScopedDaggerContribution<T>::contribution).toList(),
          pckg,
          moduleName,
        )
      )
    }

/*
    return contributionsPerModule.map { (scope, contributions) ->
      val moduleName = DaggerModuleNameGenerator.get(module, javaClass, scope.fqName)

      createGeneratedFile(codeGenDir, pckg, moduleName,
        generateScopedDaggerModuleFromContributions(
          scope,
          contributions.map(AnvilScopedDaggerContribution<T>::contribution),
          pckg,
          moduleName,
        )
      )
    }
*/
  }

  protected open fun generateScopedDaggerModuleFromContributions(
    scope: ClassReference,
    contributions: List<T>,
    pckg: String,
    moduleName: String
  ): String {
    val daggerBindings = contributionsToDaggerBindings(contributions.asSequence())

    val sortedImports = daggerBindings.imports
      .plus(scope.fqName)
      .mapTo(sortedSetOf(), FqName::asString)

    val functions = daggerBindings.functions.toList()

    return buildString(100 + sortedImports.size * 70 + functions.size * 100) {
      append("package ");append(pckg)
      append("\n\n")
      sortedImports.joinTo(this, "\nimport ", "import ")
      append("\n\n")
      append("@dagger.Module\n")
      append("@com.squareup.anvil.annotations.ContributesTo(");append(scope.shortName);append("::class)\n")
      append("abstract class ");append(moduleName);append(" {\n")

      for ((i, function) in functions.withIndex()) {
        append("  ")
        function.annotations.joinTo(this, " ")
        append(" abstract fun a");append(i.toString())
        append(function.body);append('\n')
      }
      append("}")
    }
  }

  override fun isApplicable(context: AnvilContext): Boolean = true
}