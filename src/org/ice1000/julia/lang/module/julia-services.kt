package org.ice1000.julia.lang.module

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

/**
 * @author zxj5470, ice1000
 * @date 2018/2/1
 */
interface JuliaProjectSettingsService {
	val settings: JuliaSettings
}

interface JuliaGlobalSettingsService {
	val knownJuliaExes: MutableSet<String>
}

//Project的Settings
val Project.juliaSettings: JuliaProjectSettingsService
	get() = ServiceManager.getService(this, JuliaProjectSettingsService::class.java)

//全局的Julia解释器目录
val juliaGlobalSettings: JuliaGlobalSettingsService
	get() = ServiceManager.getService(JuliaGlobalSettingsService::class.java)

//Service的实现, 基本也就这个样子
@State(
	name = "JuliaProjectSettings",
	storages = [Storage(file = "juliaConfig.xml", scheme = StorageScheme.DIRECTORY_BASED)])
class JuliaProjectSettingsServiceImpl :
	JuliaProjectSettingsService, PersistentStateComponent<JuliaSettings> {
	override val settings = JuliaSettings()
	override fun getState(): JuliaSettings? = XmlSerializerUtil.createCopy(settings)
	override fun loadState(state: JuliaSettings) {
		XmlSerializerUtil.copyBean(state, settings)
		settings.exePath.let {
			if (validateJuliaExe(it)) juliaGlobalSettings.knownJuliaExes += it
		}
	}
}

@State(
	name = "JuliaGlobalSettings",
	storages = [Storage(file = "juliaGlobalConfig.xml", scheme = StorageScheme.DIRECTORY_BASED)])
class JuliaGlobalSettingsServiceImpl :
	JuliaGlobalSettingsService, PersistentStateComponent<JuliaGlobalSettings> {
	override val knownJuliaExes: MutableSet<String> = hashSetOf()
	private fun invalidate() = knownJuliaExes.removeAll { !validateJuliaExe(it) }
	override fun getState(): JuliaGlobalSettings {
		invalidate()
		return JuliaGlobalSettings(knownJuliaExes.joinToString(File.pathSeparator))
	}

	override fun loadState(state: JuliaGlobalSettings) {
		invalidate()
		knownJuliaExes += state.allJuliaExePath.split(File.pathSeparatorChar)
	}
}