package com.infotechcorp.gradle.anvilextensions

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Set/Map  -> @Multibinds
 * Optional -> @BindsOptionalOf
 */
@Target(CLASS)
@Retention(SOURCE)
annotation class ConsumesMultibinding(
  val scope: KClass<*>
)