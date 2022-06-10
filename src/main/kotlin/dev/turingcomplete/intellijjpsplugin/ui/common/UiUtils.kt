package dev.turingcomplete.intellijjpsplugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.ui.ClickListener
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.UIManager

internal object UiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun createDefaultGridBag() = GridBag()
          .setDefaultAnchor(GridBagConstraints.NORTHWEST)
          .setDefaultInsets(0, 0, 0, 0)
          .setDefaultFill(GridBagConstraints.NONE)

  fun createCopyToClipboardButton(value: () -> String): JLabel {
    return object : JLabel(AllIcons.Actions.Copy) {

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

    fun JLabel.formatCell(tree: JTree, isSelected: Boolean, isFocused: Boolean): JLabel {
      this.foreground = UIUtil.getTreeForeground(isSelected, isFocused)
      this.background = Color.CYAN
      componentOrientation = tree.componentOrientation
      font = tree.font
      isEnabled = tree.isEnabled
      border = JBUI.Borders.empty(2, 3, 2, 3)
      return this
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object Panel {
    class TextAreaPanel(private val content: String, softWrap: Boolean = true, private var breakCommands: Boolean = false)
      : BorderLayoutPanel() {

      companion object {
        // Matches a white space and a dash if they are not in quotes: https://stackoverflow.com/a/28194133/7059880
        private val COMMAND_START_REGEX = Regex("\\s-(?=(?:\"[^\"]*\"|[^\"])*\$)")
      }

      private val textArea = JBTextArea(content).apply {
        isEditable = false
      }

      init {
        minimumSize = Dimension(150, 50)
        preferredSize = Dimension(550, 300)

        setSoftWrap(softWrap)
        setBreakCommands(breakCommands)

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