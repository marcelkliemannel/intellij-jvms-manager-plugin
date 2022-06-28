package dev.turingcomplete.intellijjpsplugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.ui.ClickListener
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.EmptyBorder

internal object UiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val EMPTY_BORDER: Border = EmptyBorder(0, 0, 0, 0)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun createDefaultGridBag() = GridBag()
          .setDefaultAnchor(GridBagConstraints.NORTHWEST)
          .setDefaultInsets(0, 0, 0, 0)
          .setDefaultFill(GridBagConstraints.NONE)

  fun createCopyToClipboardButton(value: () -> String) = object : JLabel(AllIcons.Actions.Copy) {

    init {
      object : ClickListener() {
        override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
          CopyPasteManager.getInstance().setContents(StringSelection(value()))
          return true
        }
      }.installOn(this)

      toolTipText = "Copy to Clipboard"
    }
  }

  fun createLink(title: String, url: String): HyperlinkLabel {
    return HyperlinkLabel(title).apply {
      setHyperlinkTarget(url)
    }
  }

  fun createSimpleToggleAction(text: String, icon: Icon?, isSelected: () -> Boolean, setSelected: (Boolean) -> Unit): ToggleAction {

    return object : DumbAwareToggleAction(text, "", icon) {

      override fun isSelected(e: AnActionEvent): Boolean = isSelected.invoke()

      override fun setSelected(e: AnActionEvent, state: Boolean) = setSelected.invoke(state)
    }
  }

  fun createSeparator(title: String) = SeparatorWithText().apply {
    caption = title
    setCaptionCentered(false)
  }

  fun createContextHelpLabel(text: String) = JLabel(AllIcons.General.ContextHelp).apply {
    toolTipText = text
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object Table {
    fun createContextMenuMouseListener(place: String, actionGroup: () -> ActionGroup?): MouseAdapter {
      return object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          handleMouseEvent(e)
        }

        override fun mouseReleased(e: MouseEvent) {
          handleMouseEvent(e)
        }

        private fun handleMouseEvent(e: InputEvent) {
          if (e is MouseEvent && e.isPopupTrigger) {
            actionGroup()?.let {
              ActionManager.getInstance()
                      .createActionPopupMenu(place, it).component
                      .show(e.getComponent(), e.x, e.y)
            }
          }
        }
      }
    }

    fun JLabel.formatCell(table: JTable, isSelected: Boolean, isFocused: Boolean): JLabel {
      this.foreground = UIUtil.getTableForeground(isSelected, isFocused)
      this.background = UIUtil.getTableBackground(isSelected, isFocused)
      componentOrientation = table.componentOrientation
      font = table.font
      isEnabled = table.isEnabled
      border = JBUI.Borders.empty(2, 3, 2, 3)
      return this
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object Panel {
    class TextAreaPanel(private val content: String, softWrap: Boolean = true, private var breakCommands: Boolean = false)
      : BorderLayoutPanel() {

      companion object {
        // Matches a white space and a dash if they are not in quotes: https://stackoverflow.com/a/28194133/7059880
        private val COMMAND_START_REGEX = Regex("\\s+-(?=([^\"]*\"[^\"]*\")*[^\"]*\$)")
      }

      private val textArea = JBTextArea(content).apply {
        isEditable = false
      }

      init {
        minimumSize = Dimension(150, 50)
        preferredSize = Dimension(550, 300)

        setSoftWrap(softWrap)
        setBreakCommands(breakCommands)

        textArea.caretPosition = 0

        addToCenter(ScrollPaneFactory.createScrollPane(textArea).apply {
          minimumSize = this@TextAreaPanel.minimumSize
          preferredSize = this@TextAreaPanel.preferredSize
        })
      }

      fun setBreakCommands(breakCommands: Boolean) {
        this.breakCommands = breakCommands
        textArea.text = if (breakCommands) content.replace(COMMAND_START_REGEX, " \\\\ ${System.lineSeparator()}  -") else content
      }

      fun isBreakCommands(): Boolean = breakCommands

      fun setSoftWrap(softWrap: Boolean) {
        textArea.lineWrap = softWrap
      }

      fun isSoftWrap() = textArea.lineWrap
    }
  }
}

fun GridBag.overrideLeftInset(leftInset: Int): GridBag {
  this.insets(this.insets.top, leftInset, this.insets.bottom, this.insets.right)
  return this
}

fun GridBag.overrideBottomInset(bottomInset: Int): GridBag {
  this.insets(this.insets.top, this.insets.left, bottomInset, this.insets.right)
  return this
}

fun GridBag.overrideTopInset(topInset: Int): GridBag {
  this.insets(topInset, this.insets.left, this.insets.bottom, this.insets.right)
  return this
}


fun JBLabel.copyable(): JBLabel {
  setCopyable(true)
  return this
}

fun JBLabel.xlFont(): JBLabel {
  font = font.deriveFont(UIManager.getFont("Label.font").size + JBUIScale.scale(2f))
  return this
}

fun JBLabel.xxlFont(): JBLabel {
  font = font.deriveFont(UIManager.getFont("Label.font").size + JBUIScale.scale(4f))
  return this
}

fun AnAction.toSwingAction(component: Component, eventPlace: String): AbstractAction {
  return object: AbstractAction() {

    override fun actionPerformed(e: ActionEvent) {
      val context = DataManager.getInstance().getDataContext(component)
      val event = AnActionEvent.createFromAnAction(this@toSwingAction, null, eventPlace, context)
      ActionUtil.performActionDumbAwareWithCallbacks(this@toSwingAction, event)
    }
  }
}