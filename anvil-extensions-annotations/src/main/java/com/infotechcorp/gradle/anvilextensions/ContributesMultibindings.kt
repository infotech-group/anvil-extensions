package com.infotechcorp.gradle.anvilextensions

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Same as com.squareup.anvil.annotations.ContributesMultibinding but with the ability to contribute
 * multiple IntoMap and generic types
 */
@Repeatable
@Target(CLASS)
@Retention(SOURCE)
annotation class ContributesMultibindings(
  val scope: KClass<*>,
  val boundType: KClass<*> = Unit::class,
  val boundTypeIndex: Int = -1,
  val ignoreQualifier: Boolean = false,
)