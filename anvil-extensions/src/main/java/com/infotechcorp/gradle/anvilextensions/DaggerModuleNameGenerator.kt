@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalAnvilApi::class)

package com.infotechcorp.gradle.anvilextensions

import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.capitalize
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentHashMap

object DaggerModuleNameGenerator {

  private val cache: MutableMap<Any, String> = ConcurrentHashMap()

  fun get(
    module: ModuleDescriptor,
    generator: Class<*>,
    scope: FqName,
  ): String {
    val name = module.name.asString()
    return cache.computeIfAbsent(Triple(name, generator, scope)) {
      name.toCamelCase()
        .plus(generator.simpleName.removeSuffix("CodeGenerator"))
        .filter(Char::isUpperCase)
        .plus(scope.shortName().asString())
    }
  }

  private fun String.toCamelCase(): String {
    return reversed()
      .zipWithNext()
      .mapNotNull { (l, r) ->
        when {
          l !in 'A'..'z' -> null
          r !in 'A'..'z' -> l.uppercaseChar()
          else           -> l
        }
      }
      .reversed()
      .joinToString("").capitalize()
  }
}