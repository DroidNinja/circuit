/*
 * Copyright (C) 2022 Adam Powell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key

fun interface BackStackRecordLocalProvider<in R : BackStack.Record> {
  @Composable fun providedValuesFor(record: R): ProvidedValues
}

fun interface ProvidedValues {
  @Composable fun provideValues(): List<ProvidedValue<*>>
}

class CompositeProvidedValues(private val list: List<ProvidedValues>) : ProvidedValues {
  @Composable
  override fun provideValues(): List<ProvidedValue<*>> = buildList {
    list.forEach { addAll(key(it) { it.provideValues() }) }
  }
}

@Composable
fun <R : BackStack.Record> providedValuesForBackStack(
  backStack: BackStack<R>,
  stackLocalProviders: List<BackStackRecordLocalProvider<R>> = emptyList(),
  includeDefaults: Boolean = true,
): Map<R, ProvidedValues> =
  buildMap(backStack.size) {
    backStack.forEach { record ->
      key(record) {
        put(
          record,
          CompositeProvidedValues(
            buildList(stackLocalProviders.size + 1) {
              if (includeDefaults) {
                LocalBackStackRecordLocalProviders.current.forEach {
                  add(key(it) { it.providedValuesFor(record) })
                }
              }
              stackLocalProviders.forEach { add(key(it) { it.providedValuesFor(record) }) }
            }
          )
        )
      }
    }
  }

@Suppress("RemoveExplicitTypeArguments")
val LocalBackStackRecordLocalProviders =
  compositionLocalOf<List<BackStackRecordLocalProvider<BackStack.Record>>> {
    listOf(SaveableStateRegistryBackStackRecordLocalProvider, ViewModelBackStackRecordLocalProvider)
  }
