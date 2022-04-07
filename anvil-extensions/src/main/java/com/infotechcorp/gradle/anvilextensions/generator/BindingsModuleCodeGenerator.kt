package com.infotechcorp.gradle.anvilextensions.generator

import com.infotechcorp.gradle.anvilextensions.DaggerExtensionsComposingCodeGenerator
import com.infotechcorp.gradle.anvilextensions.DaggerExtensionsModuleCodeGenerator

class BindingsModuleCodeGenerator : DaggerExtensionsComposingCodeGenerator() {

  @Suppress("UNCHECKED_CAST")
  override val generators: Sequence<DaggerExtensionsModuleCodeGenerator<Any?>> = sequenceOf(
    ContributesMultibindingsCodeGenerator(),
    ConsumesMultibindingCodeGenerator(),
  ) as Sequence<DaggerExtensionsModuleCodeGenerator<Any?>>
}