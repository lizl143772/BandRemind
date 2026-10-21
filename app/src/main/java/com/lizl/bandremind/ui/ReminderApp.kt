package com.lizl.bandremind.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull

private object Routes {
    const val HOME = "home"
    const val TEMPLATES = "templates"
    const val SETTINGS = "settings"
    const val EDIT = "edit?id={id}&template={template}"

    fun edit(ruleId: String? = null, templateId: String? = null): String {
        val query = buildList {
            if (!ruleId.isNullOrBlank()) add("id=$ruleId")
            if (!templateId.isNullOrBlank()) add("template=$templateId")
        }.joinToString("&")
        return if (query.isEmpty()) "edit" else "edit?$query"
    }
}

private const val SCHEME = "bandremind"

@Composable
fun ReminderApp(
    viewModel: ReminderViewModel = viewModel(),
    /** 应用已在前台时收到的深链，冷启动时导航层会自动处理 Activity 的 intent。 */
    incomingIntents: Flow<Intent?> = emptyFlow(),
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 应用已在前台时再收到深链：主动交给导航层跳转
    LaunchedEffect(navController) {
        incomingIntents.filterNotNull().collect { intent ->
            val ok = navController.handleDeepLink(intent)
            android.util.Log.i("BandRemind", "handleDeepLink ${intent.data} -> $ok")
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                rules = rules,
                completions = completions,
                onAdd = { navController.navigate(Routes.TEMPLATES) },
                onEdit = { id -> navController.navigate(Routes.edit(ruleId = id)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onToggle = viewModel::setEnabled,
                onDelete = { viewModel.delete(it.id) },
            )
        }

        composable(
            route = Routes.TEMPLATES,
            deepLinks = listOf(navDeepLink { uriPattern = "$SCHEME://templates" }),
        ) {
            TemplateScreen(
                onPick = { templateId ->
                    navController.navigate(Routes.edit(templateId = templateId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType; defaultValue = "" },
                navArgument("template") { type = NavType.StringType; defaultValue = "" },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "$SCHEME://edit" },
                navDeepLink { uriPattern = "$SCHEME://edit?id={id}" },
                navDeepLink { uriPattern = "$SCHEME://edit?template={template}" },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val templateId = entry.arguments?.getString("template").orEmpty()
            val existing = viewModel.ruleById(id)
            val preset = RuleTemplates.byId(templateId)?.build()

            RuleEditScreen(
                rule = existing,
                preset = preset,
                onSave = {
                    viewModel.save(it)
                    // 保存后直接回首页，避免停在模板页
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onDelete = {
                    existing?.let { viewModel.delete(it.id) }
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SETTINGS,
            deepLinks = listOf(navDeepLink { uriPattern = "$SCHEME://settings" }),
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
