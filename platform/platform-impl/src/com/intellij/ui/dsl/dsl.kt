// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.dsl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.gridLayout.JBGridLayout
import com.intellij.ui.dsl.impl.DialogPanelConfig
import com.intellij.ui.dsl.impl.PanelImpl
import org.jetbrains.annotations.ApiStatus

/*
todo
remove first/last gaps
*/

@ApiStatus.Experimental
fun panel(init: RootPanel.() -> Unit): DialogPanel {
  val dialogPanelConfig = DialogPanelConfig()
  val panel = PanelImpl(dialogPanelConfig)
  panel.init()

  val layout = JBGridLayout()
  val result = DialogPanel(layout = layout)
  panel.build(result, layout.rootGrid)
  initPanel(dialogPanelConfig, result)
  return result
}

private fun initPanel(dialogPanelConfig: DialogPanelConfig, panel: DialogPanel) {
  /* todo
  panel.preferredFocusedComponent = dialogPanelConfig.preferredFocusedComponent
  panel.validateCallbacks = dialogPanelConfig.validateCallbacks
  */
  panel.componentValidateCallbacks = dialogPanelConfig.componentValidateCallbacks
  panel.customValidationRequestors = dialogPanelConfig.customValidationRequestors
  panel.applyCallbacks = dialogPanelConfig.applyCallbacks
  panel.resetCallbacks = dialogPanelConfig.resetCallbacks
  panel.isModifiedCallbacks = dialogPanelConfig.isModifiedCallbacks
}
