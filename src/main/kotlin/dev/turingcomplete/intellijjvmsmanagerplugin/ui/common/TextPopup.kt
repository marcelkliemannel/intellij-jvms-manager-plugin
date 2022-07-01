package dev.turingcomplete.intellijjvmsmanagerplugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.vcs.commit.NonModalCommitPanel.Companion.showAbove
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.JvmsManagerToolWindowFactory
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils.createSimpleToggleAction
import java.awt.Dimension
import javax.swing.JComponent

class TextPopup private constructor(content: String, softWrap: Boolean, private val breakCommandSupported: Boolean, breakCommand: Boolean)
  : SimpleToolWindowPanel(false, true) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun showAbove(title: String,
                  content: String,
                  target: JComponent,
                  softWrap: Boolean = true,
                  breakCommandSupported: Boolean = false,
                  breakCommand: Boolean = false) {

      create(content, softWrap, breakCommandSupported, breakCommand, title).showAbove(target)
    }

    fun showCenteredInCurrentWindow(title: String,
                                    content: String,
                                    project: Project,
                                    softWrap: Boolean = true,
                                    breakCommandSupported: Boolean = false,
                                    breakCommand: Boolean = false) {

      create(content, softWrap, breakCommandSupported, breakCommand, title).showCenteredInCurrentWindow(project)
    }

    private fun create(content: String, softWrap: Boolean, breakCommandSupported: Boolean, breakCommand: Boolean, title: String): JBPopup {
      val textPopup = TextPopup(content, softWrap, breakCommandSupported, breakCommand)

      return JBPopupFactory.getInstance()
              .createComponentPopupBuilder(textPopup, textPopup)
              .setRequestFocus(true)
              .setTitle(title)
              .setFocusable(true)
              .setResizable(true)
              .setMovable(true)
              .setModalContext(false)
              .setShowShadow(true)
              .setShowBorder(true)
              .setCancelKeyEnabled(true)
              .setCancelOnClickOutside(true)
              .setCancelOnOtherWindowOpen(false)
              .createPopup()
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val textAreaPanel: UiUtils.Panel.TextAreaPanel

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createPopupToolbar()

    textAreaPanel = UiUtils.Panel.TextAreaPanel(content, softWrap, breakCommand).apply {
      val (width, height) = when {
        content.length < 100 -> Pair(500, 100)
        content.length < 400 -> Pair(600, 300)
        else -> Pair(700, 400)
      }
      preferredSize = Dimension(width, height)
    }
    setContent(textAreaPanel)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createPopupToolbar(): JComponent {
    val toolbarGroup = DefaultActionGroup().apply {
      add(createSimpleToggleAction("Soft-Wrap",
                                   AllIcons.Actions.ToggleSoftWrap,
                                   { textAreaPanel.isSoftWrap() },
                                   { textAreaPanel.setSoftWrap(it) }))
      if (breakCommandSupported) {
        add(createSimpleToggleAction("Break Command",
                                     AllIcons.Vcs.Changelist,
                                     { textAreaPanel.isBreakCommands() },
                                     { textAreaPanel.setBreakCommands(it) }))
      }
    }

    return ActionManager.getInstance()
            .createActionToolbar("${JvmsManagerToolWindowFactory.TOOLBAR_PLACE_PREFIX}.toolbar.textPopup", toolbarGroup, false)
            .run {
              setTargetComponent(this@TextPopup)
              component
            }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}