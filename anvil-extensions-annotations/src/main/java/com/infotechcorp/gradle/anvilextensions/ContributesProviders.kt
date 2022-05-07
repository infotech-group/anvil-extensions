package com.infotechcorp.gradle.anvilextensions

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

/**
 * Generate @Provides binding for each public no-arg function of a class annotated with:
 * scope annotation
 * qualifier annotation
 * @Provides
 * @MapKey
 * @IntoSet
 * @ElementsIntoSet
 *
 * Also if a class implements javax.inject.Provider, binding is generated for get() function
 */
@Repeatable
@Target(CLASS)
@Retention(SOURCE)
annotation class ContributesProviders(
  val scope: KClass<*>
)