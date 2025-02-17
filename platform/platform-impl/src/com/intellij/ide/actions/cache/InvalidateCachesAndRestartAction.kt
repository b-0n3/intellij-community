// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.cache

import com.intellij.ide.IdeBundle
import com.intellij.ide.InvalidateCacheService
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

internal class InvalidateCachesAndRestartAction : RecoveryAction {
  override val performanceRate: Int
    get() = 1
  override val presentableName: String @Nls(capitalization = Nls.Capitalization.Title)
    get() = IdeBundle.message("invalidate.all.caches.recovery.action.name")
  override val actionKey: String
    get() = "hammer"

  override fun perform(project: Project?) = invokeAndWaitIfNeeded {
    InvalidateCacheService.invalidateCachesAndRestart(project)
  }

  override fun canBeApplied(project: Project?): Boolean = true
}