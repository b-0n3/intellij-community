// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.core.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts

fun <T : Any> runInReadActionWithWriteActionPriorityWithPCE(f: () -> T): T =
    runInReadActionWithWriteActionPriority(f) ?: throw ProcessCanceledException()

fun <T : Any> runInReadActionWithWriteActionPriority(f: () -> T): T? {
    if (with(ApplicationManager.getApplication()) { isDispatchThread && isUnitTestMode }) {
        return f()
    }

    var r: T? = null
    val complete = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority {
        r = f()
    }

    if (!complete) return null
    return r!!
}

fun <T : Any> Project.runSynchronouslyWithProgress(@NlsContexts.ProgressTitle progressTitle: String, canBeCanceled: Boolean, action: () -> T): T? {
    var result: T? = null
    ProgressManager.getInstance().runProcessWithProgressSynchronously({ result = action() }, progressTitle, canBeCanceled, this)
    return result
}