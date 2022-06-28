package dev.turingcomplete.intellijjpsplugin.ui.common

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

object NotificationUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun notifyBalloon(title: String, message: String, project: Project?,
                    notificationType: NotificationType = NotificationType.INFORMATION,
                    vararg actions: AnAction) {

    ApplicationManager.getApplication().invokeLater {
      val notification = NotificationGroupManager.getInstance()
              .getNotificationGroup("JPS Plugin | Balloon")
              .createNotification(title, message, notificationType)

      actions.forEach { notification.addAction(it) }

      notification.notify(project)
    }
  }

  fun notifyOnToolWindow(message: String, project: Project?, notificationType: NotificationType = NotificationType.INFORMATION) {
    ApplicationManager.getApplication().invokeLater {
      NotificationGroupManager.getInstance()
              .getNotificationGroup("JPS Plugin | Tool Window")
              .createNotification(message, notificationType)
              .notify(project)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}