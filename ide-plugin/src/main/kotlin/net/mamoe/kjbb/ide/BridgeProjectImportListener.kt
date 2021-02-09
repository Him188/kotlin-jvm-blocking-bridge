package net.mamoe.kjbb.ide

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.ProgressIndicatorUtils
import org.jetbrains.kotlin.idea.util.projectStructure.allModules

class BridgeProjectImportListener : Disposable, ExternalSystemTaskNotificationListenerAdapter() {
    override fun dispose() {
    }


    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
            // At this point changes might be still not applied to project structure yet.
            val project = id.findResolvedProject() ?: return
            ProgressIndicatorUtils.runUnderDisposeAwareIndicator(this) {
                for (module in project.allModules()) {
                    module.getServiceIfCreated(BridgeModuleCacheService::class.java)?.initialized = false
                }
            }
        }
    }
}

internal fun ExternalSystemTaskId.findResolvedProject(): Project? {
    if (type != ExternalSystemTaskType.RESOLVE_PROJECT) return null
    return findProject()
}
