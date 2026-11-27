package com.nokia.launcher

import com.nokia.launcher.ui.home.NoticeList
import com.nokia.launcher.service.NokiaNotificationListenerService
import com.nokia.launcher.data.Notices
import com.nokia.launcher.data.Notice
import android.app.ActivityOptions
import android.content.pm.ActivityInfo
import kotlin.random.Random
import com.nokia.launcher.ui.radio.ScannerContent
import com.nokia.launcher.ui.radio.Scan
import androidx.compose.runtime.SideEffect
import android.app.KeyguardManager
import androidx.compose.ui.platform.LocalResources
import com.nokia.launcher.ui.about.AboutContent
import android.Manifest
import android.app.LocaleManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import com.nokia.launcher.ui.codes.SecretCode
import com.nokia.launcher.ui.codes.findCode
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.radio.FM_MAX
import com.nokia.launcher.ui.radio.FM_MIN
import com.nokia.launcher.ui.radio.RadioContent
import com.nokia.launcher.ui.radio.seek
import com.nokia.launcher.ui.theme.PhoneThemes
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.nokia.launcher.ui.tones.currentToneTitle
import com.nokia.launcher.ui.tones.useTone
import com.nokia.launcher.ui.tones.toneChoices
import com.nokia.launcher.ui.tones.ToneChoice
import com.nokia.launcher.ui.tones.TonePlayer
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.net.toUri
import android.media.RingtoneManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import com.nokia.launcher.ui.games.PairsGame
import com.nokia.launcher.ui.games.BantumiGame
import com.nokia.launcher.ui.games.BlocksGame
import com.nokia.launcher.ui.games.SpaceGame
import com.nokia.launcher.ui.lock.unlocks
import com.nokia.launcher.ui.lock.UNLOCK_WINDOW_MS
import com.nokia.launcher.ui.lock.LockedContent
import com.nokia.launcher.service.LockService
import com.nokia.launcher.ui.home.WhileVisible
import java.util.Date
import java.text.DateFormat
import com.nokia.launcher.ui.gallery.loadPhotos
import com.nokia.launcher.ui.gallery.PhotoContent
import com.nokia.launcher.ui.gallery.GalleryGrid
import com.nokia.launcher.data.needsSystemDialer
import com.nokia.launcher.data.loadCalls
import com.nokia.launcher.data.addContact
import com.nokia.launcher.data.CallRecord
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import com.nokia.launcher.ui.settings.ListRows
import com.nokia.launcher.ui.messages.ThreadContent
import com.nokia.launcher.ui.home.CursorText
import com.nokia.launcher.ui.games.SpiderState
import com.nokia.launcher.ui.games.SpiderContent
import com.nokia.launcher.ui.games.SPIDER_COLUMNS
import com.nokia.launcher.ui.games.PinballGame
import com.nokia.launcher.ui.games.PinballContent
import com.nokia.launcher.ui.games.MinesState
import com.nokia.launcher.ui.games.MinesContent
import com.nokia.launcher.ui.games.FlipperKeys
import com.nokia.launcher.ui.connections.connectionRows
import com.nokia.launcher.ui.appdrawer.searchKey
import com.nokia.launcher.data.sendSms
import com.nokia.launcher.data.loadSms
import com.nokia.launcher.data.loadContacts
import com.nokia.launcher.data.granted
import com.nokia.launcher.data.conversations
import com.nokia.launcher.data.contactName
import com.nokia.launcher.data.Sms
import com.nokia.launcher.data.Contact
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.foundation.layout.fillMaxWidth
import android.content.pm.PackageManager
import android.content.ContentUris
import kotlinx.coroutines.delay
import android.telephony.SmsMessage
import com.nokia.launcher.ui.home.MULTI_TAP_TIMEOUT_MS
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nokia.launcher.data.NAME_MAX
import com.nokia.launcher.data.Prefs
import com.nokia.launcher.ui.appdrawer.AppEntry
import com.nokia.launcher.ui.appdrawer.AppMenu
import com.nokia.launcher.ui.appdrawer.filterApps
import com.nokia.launcher.ui.appdrawer.loadApps
import com.nokia.launcher.ui.games.Direction
import com.nokia.launcher.ui.games.SnakeBackground
import com.nokia.launcher.ui.games.SnakeContent
import com.nokia.launcher.ui.games.SnakeSaver
import com.nokia.launcher.ui.games.SnakeState
import com.nokia.launcher.ui.home.HomeContent
import com.nokia.launcher.ui.home.InputMode
import com.nokia.launcher.ui.home.Keypad
import com.nokia.launcher.ui.home.QwertyKeyboard
import com.nokia.launcher.ui.home.dialString
import com.nokia.launcher.ui.home.dialTap
import com.nokia.launcher.ui.home.KeyFeedback
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.PhoneFrame
import com.nokia.launcher.ui.home.PhoneKey
import com.nokia.launcher.ui.home.Tap
import com.nokia.launcher.ui.home.multiTap
import com.nokia.launcher.ui.settings.EditorContent
import com.nokia.launcher.ui.settings.ListContent
import com.nokia.launcher.ui.settings.Row
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MESSAGE_MAX = 480
private const val CONTACT_NAME_MAX = 40
private const val NUMBER_MAX = 30

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(applicationContext)
        setContent { LauncherTheme(prefs.theme) { Launcher(this, prefs) } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/** A soft key: its LCD label, what it does and, optionally, what holding it does. */
private data class Soft(val label: String = "", val onLong: (() -> Unit)? = null, val onClick: () -> Unit = {})

@Composable
private fun Launcher(activity: ComponentActivity, prefs: Prefs) {
    val context = LocalContext.current
    // Follows the app language, unlike reading strings from the context.
    val resources = LocalResources.current
    // rememberSaveable: folding or rotating recreates the activity.
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var dialed by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var recipient by rememberSaveable { mutableStateOf("") }
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(InputMode.Lower) }
    var lastTap by remember { mutableStateOf<Tap?>(null) }
    // True while pressing the same key again would still change the last letter.
    var pending by remember { mutableStateOf(false) }
    LaunchedEffect(lastTap) {
        pending = lastTap != null
        delay(MULTI_TAP_TIMEOUT_MS)
        pending = false
    }
    var snake by rememberSaveable(stateSaver = SnakeSaver) {
        mutableStateOf(SnakeState.decode(prefs.snakeGame) ?: SnakeState.new())
    }
    var radioOn by rememberSaveable { mutableStateOf(false) }
    var scannerOn by rememberSaveable { mutableStateOf(false) }
    var scan by remember { mutableStateOf(Scan()) }
    var menuPressedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(menuPressedAt) {
        if (menuPressedAt != 0L) {
            delay(UNLOCK_WINDOW_MS)
            menuPressedAt = 0L
        }
    }
    var shownCode by rememberSaveable { mutableStateOf(SecretCode.SimClock) }

    var mines by rememberSaveable(stateSaver = Saver<MinesState, String>({ it.encode() }, { MinesState.decode(it) })) {
        mutableStateOf(MinesState.decode(prefs.minesGame) ?: MinesState())
    }
    var spider by rememberSaveable(stateSaver = Saver<SpiderState, String>({ it.encode() }, { SpiderState.decode(it) })) {
        mutableStateOf(SpiderState.decode(prefs.spiderGame) ?: SpiderState.new(1))
    }
    var pinball by rememberSaveable(stateSaver = Saver<PinballGame, String>({ it.encode() }, { PinballGame.decode(it) })) {
        mutableStateOf(PinballGame.decode(prefs.pinballGame))
    }
    var locked by rememberSaveable { mutableStateOf(false) }
    val flippers = remember { FlipperKeys() }
    val arcade = remember {
        listOf(SpaceGame(), BlocksGame(), BantumiGame(), PairsGame())
            .onEach { it.load(prefs.savedGame(it.id)) }
    }
    var arcadeIndex by rememberSaveable { mutableIntStateOf(0) }
    val extra = arcade[arcadeIndex]
    // Saved on leaving a game and when the app goes to the background, rotates or folds.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screen, arcadeIndex) {
        fun save() = arcade.forEach { prefs.saveGame(it.id, it.encode()) }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) save() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            save()
        }
    }
    // The Minesweeper clock runs only while the board is on screen.
    if (screen == Screen.Mines && mines.running && !locked) {
        WhileVisible(1_000) { mines = mines.copy(seconds = minOf(999, mines.seconds + 1)) }
    }
    LaunchedEffect(mines) { prefs.minesGame = mines.encode() }
    LaunchedEffect(spider) { prefs.spiderGame = spider.encode() }
    /** True while choosing who receives the message. */
    var picking by rememberSaveable { mutableStateOf(false) }
    var contactId by rememberSaveable { mutableLongStateOf(0L) }
    var threadId by rememberSaveable { mutableLongStateOf(0L) }
    var selectedNumber by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactNumber by rememberSaveable { mutableStateOf("") }
    /** In the new contact form: true while writing the number. */
    var onNumber by rememberSaveable { mutableStateOf(false) }
    var photoIndex by rememberSaveable { mutableIntStateOf(0) }

    val feedback = remember { KeyFeedback(context.applicationContext) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }

    // Which phone sound is being chosen, and the entry picked in the list.
    var toneType by rememberSaveable { mutableIntStateOf(RingtoneManager.TYPE_RINGTONE) }
    var toneKey by rememberSaveable { mutableStateOf("") }
    var playing by remember { mutableStateOf<ToneChoice?>(null) }
    val player = remember { TonePlayer(context.applicationContext) }
    val tones by produceState(emptyList<ToneChoice>(), screen == Screen.TonePick, toneType) {
        value = if (screen == Screen.TonePick) withContext(Dispatchers.IO) { toneChoices(context, toneType) }
        else emptyList()
    }
    LaunchedEffect(playing) {
        val choice = playing
        if (choice == null) {
            player.stop()
        } else {
            player.play(choice)
            // Some alarm sounds repeat without end.
            delay(10_000)
            playing = null
        }
    }
    if (screen != Screen.TonePick && playing != null) playing = null
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        player.stop()
        playing = null
    }
    DisposableEffect(player) { onDispose { player.stop() } }

    fun goHome() {
        screen = Screen.Home
        dialed = ""
        search = ""
        lastTap = null
    }

    // The system Home button, when this app is the default launcher.
    DisposableEffect(activity) {
        val listener = androidx.core.util.Consumer<Intent> {
            if (it.hasCategory(Intent.CATEGORY_HOME)) goHome()
        }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }

    fun go(target: Screen) {
        lastTap = null
        screen = target
    }

    // Only the launcher: the phone's own rotation setting is not touched. Android ignores
    // this on large screens, such as the inner one of a foldable, where both layouts fill it.
    SideEffect {
        activity.requestedOrientation = if (prefs.rotate) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // On waking, the Nokia lock is shown over Android's. This is safe only because nothing but
    // LockedContent is drawn while `locked`, and it only clears once Android is unlocked.
    SideEffect { activity.setShowWhenLocked(locked) }

    /** Locks the keypad and turns the screen off. Without the service it explains how to enable it. */
    fun lockPhone() {
        if (!LockService.isEnabled(context)) {
            go(Screen.LockSetup)
            return
        }
        radioOn = false
        scannerOn = false
        goHome()
        locked = true
        menuPressedAt = 0L
        // Set now: nothing is recomposed once the screen is off.
        activity.setShowWhenLocked(true)
        if (!LockService.screenOff()) {
            Toast.makeText(context, R.string.lock_not_ready, Toast.LENGTH_LONG).show()
        }
    }

    fun start(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    fun openDialer(number: String) =
        start(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))

    var permissionCheck by remember { mutableIntStateOf(0) }
    // Checked again on every return, since access is granted in the system settings.
    var noticeAccess by remember { mutableStateOf(NokiaNotificationListenerService.hasAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        noticeAccess = NokiaNotificationListenerService.hasAccess(context)
    }
    LaunchedEffect(Unit) {
        if (!prefs.askedNotices && !noticeAccess && screen == Screen.Home && !locked) screen = Screen.NoticeAsk
    }

    fun answerNotices(yes: Boolean) {
        prefs.askedNotices = true
        go(if (yes) Screen.NoticeAccess else Screen.Home)
    }

    fun clearNotices() {
        val keys = Notices.list.filter { it.clearable }.map { it.key }
        NokiaNotificationListenerService.dismiss(*keys.toTypedArray())
    }

    fun openNotice(notice: Notice) {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            // The user touched it, so the app that sent it may come to the front.
            @Suppress("DEPRECATION")
            options.pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        val opened = notice.open?.let {
            runCatching { it.send(context, 0, null, null, null, null, options.toBundle()) }.isSuccess
        } == true
        if (!opened) {
            Toast.makeText(context, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        } else if (notice.autoCancel) {
            NokiaNotificationListenerService.dismiss(notice.key)
        }
    }
    var asked by rememberSaveable { mutableStateOf("") }
    var numberToCall by rememberSaveable { mutableStateOf("") }

    /** Calls from the launcher; without the permission the system dialer takes over. */
    fun dial(raw: String) {
        val number = dialString(raw)
        when {
            number.isEmpty() || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
                needsSystemDialer(context, number) -> openDialer(number)
            context.granted(Manifest.permission.CALL_PHONE) ->
                start(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))
            else -> openDialer(number)
        }
    }

    fun writeMessage(to: String) {
        recipient = to
        picking = false
        mode = InputMode.Lower
        go(Screen.Message)
    }

    // Used without the SMS permission: the system messaging app sends it.
    fun sendWithSystemApp() = start(
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", recipient, null))
            .putExtra("sms_body", message),
    )

    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionCheck++
        if (asked == Manifest.permission.CALL_PHONE) {
            // Allowed or not, the call the user asked for goes ahead.
            dial(numberToCall)
        } else if (!it) {
            if (asked == Manifest.permission.SEND_SMS) {
                sendWithSystemApp()
            } else {
                // After two refusals Android stops asking; the app's settings page still allows it.
                start(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }
        }
    }

    fun ask(permission: String) {
        asked = permission
        permissionRequest.launch(permission)
    }

    /** Asks for the call permission the first time, then calls. */
    fun callNumber(number: String) {
        if (number.isNotEmpty() && !context.granted(Manifest.permission.CALL_PHONE) &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        ) {
            numberToCall = number
            ask(Manifest.permission.CALL_PHONE)
        } else {
            dial(number)
        }
    }

    fun newContact(number: String) {
        contactName = ""
        contactNumber = number
        onNumber = false
        picking = false
        mode = InputMode.Upper
        go(Screen.NewContact)
    }

    fun saveContact() {
        when {
            contactName.isBlank() || contactNumber.isBlank() ->
                Toast.makeText(context, R.string.contact_incomplete, Toast.LENGTH_SHORT).show()
            !context.granted(Manifest.permission.WRITE_CONTACTS) -> ask(Manifest.permission.WRITE_CONTACTS)
            else -> {
                val saved = addContact(context, contactName.trim(), contactNumber)
                Toast.makeText(context, if (saved) R.string.contact_saved else R.string.contact_failed, Toast.LENGTH_SHORT).show()
                if (saved) {
                    search = ""
                    go(Screen.Contacts)
                }
            }
        }
    }

    fun pickRecipient() {
        picking = true
        search = ""
        mode = InputMode.Lower
        go(Screen.Contacts)
    }

    fun send() {
        when {
            message.isBlank() -> Toast.makeText(context, R.string.message_empty, Toast.LENGTH_SHORT).show()
            recipient.isEmpty() -> pickRecipient()
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) -> sendWithSystemApp()
            !context.granted(Manifest.permission.SEND_SMS) -> ask(Manifest.permission.SEND_SMS)
            // An SMS can cost money, so it is always confirmed.
            else -> go(Screen.SendConfirm)
        }
    }

    fun confirmSend() {
        val text = message
        val appContext = context.applicationContext
        sendSms(appContext, recipient, text) { sent ->
            Toast.makeText(appContext, if (sent) R.string.sms_sent else R.string.sms_failed, Toast.LENGTH_LONG).show()
            // A failed message stays as a draft.
            if (sent && message == text) message = ""
        }
        go(Screen.Messages)
    }

    fun openCode(code: SecretCode) {
        dialed = ""
        code.systemScreen?.let { action ->
            if (code == SecretCode.TestMenu) {
                Toast.makeText(context, resources.getString(R.string.code_test_dial, code.code), Toast.LENGTH_LONG).show()
            }
            start(Intent(action))
            return
        }
        when (code) {
            // Only the launcher's own settings: a code must never wipe the phone.
            SecretCode.SoftReset, SecretCode.Format -> go(Screen.Restore)
            SecretCode.Scanner -> {
                radioOn = false
                scannerOn = true
                scan = scan.copy(held = false)
                go(Screen.Scanner)
            }
            else -> {
                shownCode = code
                go(Screen.Code)
            }
        }
    }

    fun scannerKey(char: Char) {
        when (char.lowercaseChar()) {
            '4', 'a' -> scan = scan.tune(-1)
            '6', 'd' -> scan = scan.tune(1)
            '#' -> scan = scan.copy(held = !scan.held)
            '5', ' ' -> scannerOn = !scannerOn
        }
    }

    fun tune(frequency: Int) {
        prefs.radioFrequency = when {
            frequency > FM_MAX -> FM_MIN
            frequency < FM_MIN -> FM_MAX
            else -> frequency
        }
    }

    fun radioKey(char: Char) {
        when (char.lowercaseChar()) {
            '4', 'a' -> tune(prefs.radioFrequency - 1)
            '6', 'd' -> tune(prefs.radioFrequency + 1)
            '2', 'w' -> tune(seek(prefs.radioFrequency, up = true))
            '8', 's' -> tune(seek(prefs.radioFrequency, up = false))
            '5', ' ' -> radioOn = !radioOn
        }
    }

    fun open(app: AppEntry) {
        // The exact activity, since one package can expose several launcher icons.
        start(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
        goHome()
    }

    // Reloaded each time they are shown, so installs and removals show up.
    val needsApps = screen == Screen.Apps || screen == Screen.GoTo
    val apps by produceState(emptyList<AppEntry>(), needsApps) {
        if (needsApps) value = withContext(Dispatchers.IO) { loadApps(context) }
    }
    val shown = filterApps(apps, search)

    fun toggleFavorite(app: AppEntry) {
        val id = app.component.flattenToString()
        val added = id !in prefs.favorites
        prefs.favorites = if (added) prefs.favorites + id else prefs.favorites - id
        Toast.makeText(
            context,
            resources.getString(if (added) R.string.favorite_added else R.string.favorite_removed, app.label),
            Toast.LENGTH_SHORT,
        ).show()
    }

    // shortcut: reads the newest 1000 SMS; page by conversation if older ones are needed.
    val inMessages = screen == Screen.Messages || screen == Screen.Thread
    var sms by remember { mutableStateOf(emptyList<Sms>() to emptyMap<String, String>()) }
    // Re-read every few seconds to pick up new messages, and only while they are on screen.
    if (inMessages) {
        WhileVisible(5_000, screen, permissionCheck) {
            if (context.granted(Manifest.permission.READ_SMS)) {
                sms = withContext(Dispatchers.IO) {
                    val list = loadSms(context)
                    list to conversations(list).mapNotNull { m ->
                        contactName(context, m.address)?.let { m.address to it }
                    }.toMap()
                }
            }
        }
    }
    val threadMessages = sms.first.filter { it.thread == threadId }
    val threadAddress = threadMessages.firstOrNull()?.address.orEmpty()

    val needsContacts = screen == Screen.Contacts || screen == Screen.Contact
    val contacts by produceState(emptyList<Contact>(), needsContacts, permissionCheck) {
        if (needsContacts && context.granted(Manifest.permission.READ_CONTACTS)) {
            value = withContext(Dispatchers.IO) { loadContacts(context) }
        }
    }
    val contact = contacts.firstOrNull { it.id == contactId }

    val needsCalls = screen == Screen.CallLog
    val calls by produceState(emptyList<CallRecord>(), needsCalls, permissionCheck) {
        if (needsCalls && context.granted(Manifest.permission.READ_CALL_LOG)) {
            value = withContext(Dispatchers.IO) { loadCalls(context) }
        }
    }

    val photoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val needsPhotos = screen == Screen.Gallery || screen == Screen.Photo
    val photos by produceState(emptyList<Uri>(), needsPhotos, permissionCheck) {
        if (needsPhotos && context.granted(photoPermission)) {
            value = withContext(Dispatchers.IO) { loadPhotos(context) }
        }
    }

    fun movePhoto(step: Int) {
        if (photos.isNotEmpty()) photoIndex = (photoIndex + step + photos.size) % photos.size
    }

    fun call() {
        when (screen) {
            Screen.Thread -> callNumber(threadAddress)
            Screen.Contact -> callNumber(contact?.numbers?.firstOrNull().orEmpty())
            Screen.Number -> callNumber(selectedNumber)
            // As on a Nokia, the green key with nothing dialed shows the last calls.
            else -> if (dialed.isEmpty()) go(Screen.CallLog) else callNumber(dialed)
        }
    }

    fun openContacts() {
        picking = false
        search = ""
        mode = InputMode.Lower
        go(Screen.Contacts)
    }

    fun openApps() {
        search = ""
        go(Screen.Apps)
    }

    val roles = remember { context.getSystemService(RoleManager::class.java) }
    var roleCheck by remember { mutableIntStateOf(0) }
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleCheck++ // re-reads isRoleHeld
    }
    val isDefault = remember(roleCheck, screen) { roles?.isRoleHeld(RoleManager.ROLE_HOME) == true }

    fun chooseLauncher() {
        if (!isDefault && roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            start(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    fun setLanguage(tag: String) {
        // shortcut: per-app language needs Android 13; add AppCompat only if Android 12 matters.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(tag)
        } else {
            Toast.makeText(context, R.string.language_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    fun editName() {
        nameDraft = prefs.phoneName
        mode = InputMode.Upper
        go(Screen.Name)
    }

    val brand = stringResource(R.string.brand)
    val keySoundNames = listOf(R.string.off, R.string.key_sound_phone, R.string.key_sound_beep, R.string.key_sound_click)
        .map { stringResource(it) }

    fun applyTone() {
        val choice = tones.firstOrNull { it.key == toneKey } ?: return
        if (!Settings.System.canWrite(context)) {
            Toast.makeText(context, R.string.tone_allow, Toast.LENGTH_LONG).show()
            start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))
            return
        }
        playing = null
        val done = useTone(context, choice, toneType)
        Toast.makeText(context, if (done) R.string.tone_changed else R.string.tone_failed, Toast.LENGTH_SHORT).show()
        if (done) go(Screen.Tones)
    }
    val themeNames = PhoneThemes.map { stringResource(it.name) }
    val onOff = @Composable { on: Boolean -> stringResource(if (on) R.string.on else R.string.off) }

    val useNumber = search.takeIf { picking && it.length >= 3 && it.all { c -> c.isDigit() || c in "+*#" } }

    val rows: List<Row> = when (screen) {
        // The launcher's own sections, then the user's favorite apps. `Menu` is only for apps.
        Screen.GoTo -> listOf(
            Row(stringResource(R.string.menu_messages)) { go(Screen.Messages) },
            Row(stringResource(R.string.contacts)) { openContacts() },
            Row(stringResource(R.string.menu_call_log)) { go(Screen.CallLog) },
            Row(stringResource(R.string.games)) { go(Screen.Games) },
            Row(stringResource(R.string.menu_gallery)) { go(Screen.Gallery) },
            Row(stringResource(R.string.radio)) { go(Screen.Radio) },
            Row(stringResource(R.string.connections)) { go(Screen.Connections) },
            Row(stringResource(R.string.secret_codes)) { go(Screen.Codes) },
            Row(stringResource(R.string.notifications)) {
                go(if (noticeAccess) Screen.Notifications else Screen.NoticeAccess)
            },
        ) + prefs.favorites.mapNotNull { id ->
            apps.firstOrNull { it.component.flattenToString() == id }
        }.map { app -> Row("★ ${app.label}") { open(app) } }
        Screen.Messages -> listOf(Row(stringResource(R.string.write_message)) { writeMessage("") }) +
            if (context.granted(Manifest.permission.READ_SMS)) {
                conversations(sms.first).map { last ->
                    Row(sms.second[last.address] ?: last.address, last.body.replace('\n', ' ').take(60)) {
                        threadId = last.thread
                        go(Screen.Thread)
                    }
                }
            } else {
                listOf(
                    Row(stringResource(R.string.sms_allow), stringResource(R.string.sms_allow_detail)) {
                        ask(Manifest.permission.READ_SMS)
                    },
                    Row(stringResource(R.string.sms_system_app)) {
                        start(
                            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        Screen.Contacts -> buildList {
            useNumber?.let { number ->
                add(Row(stringResource(R.string.use_number), number) { writeMessage(number) })
            }
            if (!context.granted(Manifest.permission.READ_CONTACTS)) {
                add(Row(stringResource(R.string.contacts_allow)) { ask(Manifest.permission.READ_CONTACTS) })
            }
            val wanted = searchKey(search)
            contacts.filter { searchKey(it.name).contains(wanted) || it.numbers.any { n -> search in n } }
                .forEach { found ->
                    add(Row(found.name, found.numbers.first()) {
                        if (picking && found.numbers.size == 1) {
                            writeMessage(found.numbers.first())
                        } else {
                            contactId = found.id
                            go(Screen.Contact)
                        }
                    })
                }
        }
        Screen.CallLog -> if (!context.granted(Manifest.permission.READ_CALL_LOG)) {
            listOf(Row(stringResource(R.string.calls_allow)) { ask(Manifest.permission.READ_CALL_LOG) })
        } else if (calls.isEmpty()) {
            listOf(Row(stringResource(R.string.calls_empty)) {})
        } else {
            calls.map { record ->
                val kind = stringResource(
                    when (record.type) {
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> R.string.call_outgoing
                        android.provider.CallLog.Calls.MISSED_TYPE -> R.string.call_missed
                        android.provider.CallLog.Calls.REJECTED_TYPE -> R.string.call_rejected
                        else -> R.string.call_incoming
                    },
                )
                val whenText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(record.time))
                val hidden = stringResource(R.string.call_private)
                Row(record.name ?: record.number.ifEmpty { hidden }, "$kind · $whenText") {
                    if (record.number.isNotEmpty()) {
                        selectedNumber = record.number
                        go(Screen.Number)
                    }
                }
            }
        }
        Screen.Number -> listOf(
            Row(stringResource(R.string.soft_call), selectedNumber) { callNumber(selectedNumber) },
            Row(stringResource(R.string.write_message)) { writeMessage(selectedNumber) },
            Row(stringResource(R.string.contact_save)) { newContact(selectedNumber) },
        )
        Screen.Contact -> contact?.let { found ->
            if (picking) {
                found.numbers.map { number -> Row(number) { writeMessage(number) } }
            } else {
                found.numbers.flatMap { number ->
                    listOf(
                        Row(stringResource(R.string.soft_call), number) { callNumber(number) },
                        Row(stringResource(R.string.write_message), number) { writeMessage(number) },
                    )
                } + Row(stringResource(R.string.contact_edit)) {
                    start(
                        Intent(
                            Intent.ACTION_EDIT,
                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, found.id),
                        ),
                    )
                }
            }
        }.orEmpty()
        Screen.Games -> listOf(
            Row(
                stringResource(R.string.snake),
                if (snake.inProgress) stringResource(R.string.snake_continue, snake.score)
                else stringResource(R.string.snake_best, prefs.snakeBest),
            ) {
                // An unfinished game continues; "New" starts over.
                snake = if (snake.over) SnakeState.new() else snake.copy(paused = true)
                go(Screen.Snake)
            },
            Row(stringResource(R.string.pinball), stringResource(R.string.snake_best, prefs.pinballBest)) {
                if (pinball.over) pinball = PinballGame()
                go(Screen.Pinball)
            },
            Row(stringResource(R.string.spider), stringResource(R.string.spider_suits, spider.suits)) {
                if (spider.won) go(Screen.SpiderLevel) else go(Screen.Spider)
            },
            Row(stringResource(R.string.mines)) {
                if (mines.over) mines = MinesState()
                go(Screen.Mines)
            },
        ) + arcade.mapIndexed { index, game ->
            Row(stringResource(game.title), stringResource(R.string.snake_best, prefs.record(game.id))) {
                arcadeIndex = index
                go(Screen.Arcade)
            }
        }
        Screen.SpiderLevel -> listOf(1, 2, 4).map { suits ->
            Row(stringResource(R.string.spider_suits, suits)) {
                spider = SpiderState.new(suits)
                go(Screen.Spider)
            }
        }
        Screen.Settings -> listOf(
            Row(
                stringResource(R.string.default_launcher),
                stringResource(
                    if (isDefault) R.string.default_launcher_active else R.string.default_launcher_select,
                ),
            ) { chooseLauncher() },
            Row(stringResource(R.string.phone_theme), stringResource(R.string.phone_theme_detail)) {
                go(Screen.Theme)
            },
            Row(stringResource(R.string.phone_name), stringResource(R.string.phone_name_detail)) { editName() },
            Row(
                stringResource(R.string.keyboard),
                stringResource(if (prefs.qwerty) R.string.keyboard_qwerty else R.string.keyboard_classic),
            ) { prefs.qwerty = !prefs.qwerty },
            Row(
                stringResource(R.string.rotation),
                stringResource(if (prefs.rotate) R.string.rotation_on else R.string.rotation_off),
            ) { prefs.rotate = !prefs.rotate },
            Row(
                stringResource(R.string.notifications),
                stringResource(if (noticeAccess) R.string.notices_on else R.string.notices_off),
            ) { go(Screen.NoticeAccess) },
            Row(stringResource(R.string.tones)) { go(Screen.Tones) },
            Row(stringResource(R.string.language)) { go(Screen.Language) },
            Row(stringResource(R.string.time_date)) { start(Intent(Settings.ACTION_DATE_SETTINGS)) },
            Row(stringResource(R.string.restore)) { go(Screen.Restore) },
            Row(stringResource(R.string.about)) { go(Screen.About) },
        )
        Screen.Theme -> themeNames.mapIndexed { index, name ->
            Row(if (index == prefs.theme) "$name *" else name, swatch = PhoneThemes[index].lcd) {
                prefs.theme = index
            }
        }
        Screen.Tones -> listOf(
            RingtoneManager.TYPE_RINGTONE to R.string.tone_ring,
            RingtoneManager.TYPE_NOTIFICATION to R.string.tone_message,
            RingtoneManager.TYPE_ALARM to R.string.tone_alarm,
        ).map { (type, label) ->
            Row(stringResource(label), currentToneTitle(context, type) ?: stringResource(R.string.tone_silent)) {
                toneType = type
                toneKey = ""
                go(Screen.TonePick)
            }
        } + listOf(
            Row(stringResource(R.string.keypad_tone), keySoundNames[if (prefs.keyTone) prefs.keySound + 1 else 0]) {
                go(Screen.KeySound)
            },
            Row(stringResource(R.string.keypad_vibrate), onOff(prefs.keyVibrate)) {
                prefs.keyVibrate = !prefs.keyVibrate
            },
        )
        // The first entry turns the keypad sound off; each other one is played when chosen.
        Screen.KeySound -> keySoundNames.mapIndexed { index, name ->
            val current = if (prefs.keyTone) prefs.keySound + 1 else 0
            Row(name, if (index == current) stringResource(R.string.tone_current) else null) {
                prefs.keyTone = index > 0
                if (index > 0) {
                    prefs.keySound = index - 1
                    feedback.play('5', true, false, index - 1)
                }
            }
        }
        Screen.TonePick -> {
            val current = currentToneTitle(context, toneType)
            tones.map { choice ->
                val mark = when {
                    choice.key == playing?.key -> R.string.tone_playing
                    choice.key == toneKey -> R.string.tone_picked
                    choice.title == current -> R.string.tone_current
                    else -> null
                }
                Row(choice.title, mark?.let { stringResource(it) }) {
                    toneKey = choice.key
                    playing = choice
                }
            }
        }
        Screen.Language -> listOf(
            Row(stringResource(R.string.language_system)) { setLanguage("") },
            Row(stringResource(R.string.language_english)) { setLanguage("en") },
            Row(stringResource(R.string.language_spanish)) { setLanguage("es") },
        )
        Screen.Codes -> SecretCode.entries.map { code ->
            Row(code.code, stringResource(code.title)) { openCode(code) }
        }
        Screen.Connections -> connectionRows(::start, ::ask)
        Screen.Notifications -> Notices.list.map { notice ->
            val head = if (notice.title.isBlank()) notice.app else "${notice.app}: ${notice.title}"
            Row(head, notice.text) { openNotice(notice) }
        }
        else -> emptyList()
    }

    // Contacts opened to choose a recipient go back to the message.
    fun previous() = if (picking && screen == Screen.Contacts) Screen.Message else screen.parent()
    val back = Soft(stringResource(R.string.soft_back)) { go(previous()) }
    val clearLabel = stringResource(R.string.soft_clear)
    // The right soft key deletes while there is text, and goes back once it is empty.
    fun clearOrBack(text: String, set: (String) -> Unit) =
        if (text.isEmpty()) back
        else Soft(clearLabel, onLong = { set("") }) { set(text.dropLast(1)); lastTap = null }

    // QWERTY has its own shift key, so abc/ABC/123 is only for the classic keypad.
    val modeKey = if (prefs.qwerty) Soft() else Soft(mode.label) {
        mode = InputMode.entries[(mode.ordinal + 1) % InputMode.entries.size]
        lastTap = null
    }

    fun gameKey(char: Char) {
        val key = char.lowercaseChar()
        when (screen) {
            Screen.Mines -> mines = when (key) {
                '2', 'w' -> mines.move(0, -1)
                '8', 's' -> mines.move(0, 1)
                '4', 'a' -> mines.move(-1, 0)
                '6', 'd' -> mines.move(1, 0)
                '5', ' ' -> if (mines.marking) mines.flag(mines.cursor) else mines.reveal(mines.cursor)
                '#', 'f' -> mines.flag(mines.cursor)
                else -> mines
            }
            Screen.Spider -> spider = when {
                key.isDigit() -> spider.tapColumn((key - '1' + SPIDER_COLUMNS) % SPIDER_COLUMNS)
                key == '*' || key == 'u' -> spider.undo()
                key == '#' || key == ' ' -> spider.deal()
                else -> spider
            }
            Screen.Pinball -> when (key) {
                '4', '*', '1', '7', 'a', 'z' -> flippers.left()
                '6', '#', '3', '9', 'l', 'm' -> flippers.right()
                '5', '2', '8', '0', ' ' -> pinball.launch()
            }
            Screen.Arcade -> extra.key(char)
            else -> {}
        }
    }

    val softKeys: List<Soft> = if (locked) {
        listOf(Soft(), Soft(stringResource(R.string.soft_unlock)) { menuPressedAt = System.currentTimeMillis() }, Soft())
    } else when (screen) {
        Screen.LockSetup -> listOf(
            Soft(stringResource(R.string.soft_activate)) { start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            Soft(),
            Soft(stringResource(R.string.soft_back)) { go(Screen.Apps) },
        )
        Screen.Home -> if (dialed.isEmpty()) {
            listOf(
                Soft(stringResource(R.string.soft_go_to)) { go(Screen.GoTo) },
                Soft(stringResource(R.string.soft_menu)) { openApps() },
                Soft(stringResource(R.string.soft_setting)) { go(Screen.Settings) },
            )
        } else {
            listOf(
                Soft(stringResource(R.string.soft_message)) { writeMessage(dialed.filter { it !in "pw" }) },
                Soft(stringResource(R.string.soft_call)) { call() },
                clearOrBack(dialed) { dialed = it },
            )
        }
        Screen.NoticeAsk -> listOf(
            Soft(stringResource(R.string.soft_no)) { answerNotices(false) },
            Soft(),
            Soft(stringResource(R.string.soft_yes)) { answerNotices(true) },
        )
        Screen.Notifications -> listOf(
            if (Notices.list.any { it.clearable }) {
                Soft(stringResource(R.string.notices_clear)) { clearNotices() }
            } else {
                Soft()
            },
            Soft(),
            back,
        )
        Screen.NoticeAccess -> listOf(
            Soft(),
            Soft(stringResource(R.string.soft_open)) {
                start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            back,
        )
        Screen.GoTo -> listOf(
            Soft(stringResource(R.string.soft_add)) { search = ""; go(Screen.Apps) },
            Soft(),
            back,
        )
        Screen.Messages -> listOf(
            Soft(stringResource(R.string.soft_write)) { writeMessage("") },
            Soft(),
            back,
        )
        Screen.Thread -> listOf(
            Soft(stringResource(R.string.soft_reply)) { writeMessage(threadAddress) },
            Soft(stringResource(R.string.soft_call)) { call() },
            back,
        )
        Screen.SendConfirm -> listOf(Soft(stringResource(R.string.soft_yes)) { confirmSend() }, Soft(), back)
        Screen.Contacts -> listOf(
            // While choosing a recipient a number may be typed; otherwise a contact can be added.
            if (picking) modeKey else Soft(stringResource(R.string.soft_new)) { newContact("") },
            Soft(stringResource(R.string.soft_open)) { rows.firstOrNull()?.onClick?.invoke() },
            clearOrBack(search) { search = it },
        )
        Screen.NewContact -> {
            val text = if (onNumber) contactNumber else contactName
            listOf(
                Soft(stringResource(R.string.soft_save)) { saveContact() },
                Soft(stringResource(R.string.soft_next)) {
                    onNumber = !onNumber
                    lastTap = null
                    mode = if (onNumber) InputMode.Number else InputMode.Upper
                },
                clearOrBack(text) { if (onNumber) contactNumber = it else contactName = it },
            )
        }
        Screen.Gallery -> listOf(Soft(), Soft(), back)
        Screen.Photo -> listOf(
            Soft(stringResource(R.string.soft_previous)) { movePhoto(-1) },
            Soft(stringResource(R.string.soft_next)) { movePhoto(1) },
            back,
        )
        Screen.Spider -> listOf(
            Soft(stringResource(R.string.soft_new)) { go(Screen.SpiderLevel) },
            Soft(stringResource(R.string.soft_deal)) { spider = spider.deal() },
            back,
        )
        Screen.Mines -> listOf(
            Soft(stringResource(R.string.soft_new)) { mines = MinesState() },
            Soft(stringResource(if (mines.marking) R.string.soft_mark_off else R.string.soft_mark)) {
                mines = mines.copy(marking = !mines.marking)
            },
            back,
        )
        Screen.Arcade -> listOf(
            Soft(stringResource(R.string.soft_new)) { extra.restart() },
            extra.actionLabel?.let { Soft(stringResource(it)) { extra.action() } } ?: Soft(),
            back,
        )
        Screen.Pinball -> listOf(
            Soft(stringResource(R.string.soft_new)) { pinball = PinballGame() },
            Soft(stringResource(R.string.soft_launch)) { pinball.launch() },
            back,
        )
        Screen.Apps -> listOf(
            // Acts on the highlighted app; holding any other app does the same.
            Soft(stringResource(R.string.soft_favorite)) { shown.firstOrNull()?.let(::toggleFavorite) },
            Soft(stringResource(R.string.soft_open)) { shown.firstOrNull()?.let(::open) },
            clearOrBack(search) { search = it },
        )
        Screen.Message -> listOf(
            Soft(stringResource(R.string.soft_send)) { send() },
            modeKey,
            clearOrBack(message) { message = it },
        )
        Screen.Name -> listOf(
            Soft(stringResource(R.string.soft_save)) { prefs.phoneName = nameDraft; go(Screen.Settings) },
            modeKey,
            clearOrBack(nameDraft) { nameDraft = it },
        )
        Screen.Snake -> listOf(
            Soft(stringResource(R.string.soft_new)) { snake = SnakeState.new() },
            Soft(stringResource(R.string.soft_pause)) {
                snake = if (snake.over) SnakeState.new() else snake.copy(paused = !snake.paused)
            },
            back,
        )
        Screen.TonePick -> listOf(
            Soft(stringResource(R.string.soft_stop)) { playing = null },
            Soft(stringResource(R.string.soft_choose)) { applyTone() },
            back,
        )
        Screen.Restore -> listOf(
            Soft(stringResource(R.string.soft_yes)) { prefs.reset(); go(Screen.Settings) },
            Soft(),
            back,
        )
        Screen.Scanner -> listOf(
            Soft(stringResource(if (scan.held) R.string.scanner_search else R.string.scanner_hold)) {
                scan = scan.copy(held = !scan.held)
            },
            Soft(stringResource(if (scannerOn) R.string.off_action else R.string.on_action)) {
                scannerOn = !scannerOn
            },
            back,
        )
        Screen.Radio -> listOf(
            Soft(stringResource(R.string.soft_station)) { tune(seek(prefs.radioFrequency, up = true)) },
            Soft(stringResource(if (radioOn) R.string.off_action else R.string.on_action)) {
                radioOn = !radioOn
            },
            back,
        )
        else -> listOf(Soft(), Soft(), back)
    }

    /** Applies one keypad press to a text field. */
    fun type(text: String, char: Char, max: Int, textMode: InputMode): String {
        val tap = Tap(char, System.currentTimeMillis())
        val result = multiTap(text, lastTap, tap, textMode)
        lastTap = tap
        return if (result.length > max) text else result
    }

    /** While locked only the unlock combination does anything. */
    fun lockedKey(char: Char) {
        if (unlocks(menuPressedAt, char, System.currentTimeMillis())) {
            menuPressedAt = 0L
            val keyguard = context.getSystemService(KeyguardManager::class.java)
            if (!keyguard.isKeyguardLocked) {
                locked = false
            } else {
                // Android asks for the fingerprint or PIN; cancelling stays on the Nokia lock.
                keyguard.requestDismissKeyguard(
                    activity,
                    object : KeyguardManager.KeyguardDismissCallback() {
                        override fun onDismissSucceeded() { locked = false }
                    },
                )
            }
        }
    }

    fun onChar(char: Char) {
        if (locked) return lockedKey(char)
        // Menu and then `*` locks, as on the classic phones. With nothing typed, `*` has no other use.
        if (screen == Screen.Apps && search.isEmpty() && char == '*') return lockPhone()
        when (screen) {
            // `*` and `#` are real characters while dialing (service codes); `*` also gives + p w.
            Screen.Home -> {
                val tap = Tap(char, System.currentTimeMillis())
                dialed = dialTap(dialed, lastTap, tap)
                lastTap = tap
                findCode(dialed)?.let(::openCode)
            }
            Screen.Apps ->
                if (char == '#') shown.firstOrNull()?.let(::open)
                else search = type(search, char, NAME_MAX * 2, InputMode.Lower)
            Screen.Contacts ->
                if (char == '#') modeKey.onClick() else search = type(search, char, NAME_MAX * 2, mode)
            Screen.Mines, Screen.Spider, Screen.Pinball, Screen.Arcade -> gameKey(char)
            Screen.Photo -> if (char == '4') movePhoto(-1) else if (char == '6') movePhoto(1)
            Screen.NewContact ->
                if (onNumber) {
                    if (contactNumber.length < NUMBER_MAX) contactNumber += char
                } else if (char == '#') {
                    modeKey.onClick()
                } else {
                    contactName = type(contactName, char, CONTACT_NAME_MAX, mode)
                }
            Screen.Message ->
                if (char == '#') modeKey.onClick() else message = type(message, char, MESSAGE_MAX, mode)
            Screen.Name ->
                if (char == '#') modeKey.onClick() else nameDraft = type(nameDraft, char, NAME_MAX, mode)
            Screen.Snake -> when (char) {
                '2' -> snake = snake.turn(Direction.Up)
                '4' -> snake = snake.turn(Direction.Left)
                '6' -> snake = snake.turn(Direction.Right)
                '8' -> snake = snake.turn(Direction.Down)
                '5' -> softKeys[1].onClick()
            }
            Screen.Radio -> radioKey(char)
            Screen.Scanner -> scannerKey(char)
            else -> rows.getOrNull((char.digitToIntOrNull() ?: 0) - 1)?.onClick?.invoke()
        }
    }

    /** Holding a key writes its number; holding 0 while dialing writes `+`. */
    fun onLongChar(char: Char) {
        if (locked) return lockedKey(char)
        lastTap = null
        when (screen) {
            Screen.Home -> dialed += if (char == '0') '+' else char
            Screen.NewContact -> if (onNumber) {
                if (contactNumber.length < NUMBER_MAX) contactNumber += if (char == '0') '+' else char
            } else if (char.isDigit() && contactName.length < CONTACT_NAME_MAX) {
                contactName += char
            }
            Screen.Apps, Screen.Contacts -> if (char.isDigit()) search += char
            Screen.Message -> if (char.isDigit() && message.length < MESSAGE_MAX) message += char
            Screen.Name -> if (char.isDigit() && nameDraft.length < NAME_MAX) nameDraft += char
            else -> onChar(char)
        }
    }

    /** A character from the QWERTY keyboard, which needs no multi-tap. */
    fun onText(char: Char) {
        if (locked) return lockedKey(char)
        if (screen == Screen.Apps && search.isEmpty() && char == '*') return lockPhone()
        lastTap = null
        when (screen) {
            Screen.Home -> when {
                char.isDigit() || char in "*#+" -> {
                    dialed += char
                    findCode(dialed)?.let(::openCode)
                }
                // Typing a letter on the home screen searches the apps, as on QWERTY Nokias.
                char.isLetter() -> {
                    search = char.toString()
                    go(Screen.Apps)
                }
            }
            Screen.Apps, Screen.Contacts -> if (search.length < NAME_MAX * 2) search += char
            Screen.Mines, Screen.Spider, Screen.Pinball, Screen.Arcade -> gameKey(char)
            Screen.NewContact -> if (onNumber) {
                if ((char.isDigit() || char in "*#+") && contactNumber.length < NUMBER_MAX) contactNumber += char
            } else if (contactName.length < CONTACT_NAME_MAX) {
                contactName += char
            }
            Screen.Message -> if (message.length < MESSAGE_MAX) message += char
            Screen.Name -> if (nameDraft.length < NAME_MAX) nameDraft += char
            Screen.Snake -> when (char.lowercaseChar()) {
                'w', '2' -> snake = snake.turn(Direction.Up)
                'a', '4' -> snake = snake.turn(Direction.Left)
                'd', '6' -> snake = snake.turn(Direction.Right)
                's', 'x', '8' -> snake = snake.turn(Direction.Down)
                ' ', '5' -> softKeys[1].onClick()
            }
            Screen.Radio -> radioKey(char)
            Screen.Scanner -> scannerKey(char)
            else -> rows.getOrNull((char.digitToIntOrNull() ?: 0) - 1)?.onClick?.invoke()
        }
    }

    fun onDelete(all: Boolean) {
        if (locked) return
        lastTap = null
        fun cut(text: String) = if (all) "" else text.dropLast(1)
        when (screen) {
            Screen.Home -> dialed = cut(dialed)
            Screen.Apps, Screen.Contacts -> search = cut(search)
            Screen.Message -> message = cut(message)
            Screen.NewContact -> if (onNumber) contactNumber = cut(contactNumber) else contactName = cut(contactName)
            Screen.Name -> nameDraft = cut(nameDraft)
            else -> {}
        }
    }

    fun onEnter() {
        if (locked) return
        when (screen) {
            Screen.Home -> if (dialed.isNotEmpty()) call()
            Screen.Apps -> shown.firstOrNull()?.let(::open)
            Screen.Message -> onText('\n')
            Screen.Contacts, Screen.NewContact -> softKeys[1].onClick()
            Screen.SendConfirm -> softKeys[0].onClick()
            Screen.Name, Screen.Restore, Screen.Snake -> softKeys[if (screen == Screen.Snake) 1 else 0].onClick()
            else -> {}
        }
    }

    LaunchedEffect(snake.score) {
        if (snake.score > prefs.snakeBest) prefs.snakeBest = snake.score
    }

    // As the home screen there is nowhere to go back to, so Back stays on Home.
    BackHandler {
        when {
            locked -> Unit
            // Leaving the question without answering counts as "No".
            screen == Screen.NoticeAsk -> answerNotices(false)
            screen == Screen.Home -> dialed = ""
            else -> go(previous())
        }
    }

    val colors = LocalPhoneColors.current
    fun feel(char: Char?) {
        val muted = !locked && screen.isGame && !prefs.gameSound
        feedback.play(char, prefs.keyTone && !muted, prefs.keyVibrate, prefs.keySound)
    }

    fun onPhoneKey(key: PhoneKey) {
        feel(null)
        when (key) {
            PhoneKey.SoftLeft -> softKeys[0].onClick()
            PhoneKey.Center -> softKeys[1].onClick()
            PhoneKey.SoftRight -> softKeys[2].onClick()
            // While playing, the green key switches the sound instead of leaving for the call log.
            PhoneKey.Call -> if (!locked) {
                if (screen.isGame) prefs.gameSound = !prefs.gameSound else call()
            }
            PhoneKey.End -> if (!locked) goHome()
        }
    }

    fun onLongPhoneKey(key: PhoneKey) {
        feel(null)
        if (key == PhoneKey.SoftRight) (softKeys[2].onLong ?: softKeys[2].onClick)()
    }

    // Games use the whole screen: their soft-key names are written on the keys themselves.
    val inGame = !locked && screen.isGame
    val navLabels = if (inGame) softKeys.map { it.label } else null
    val soundLabel = when {
        !inGame -> null
        // With key sounds off in Tones there is nothing to switch.
        !prefs.keyTone -> "—"
        else -> stringResource(if (prefs.gameSound) R.string.game_sound_on else R.string.game_sound_off)
    }

    PhoneFrame(
        keyboard = { keyboardModifier ->
            if (prefs.qwerty) {
                QwertyKeyboard(
                    onKey = ::onPhoneKey,
                    onText = { feel(it); onText(it) },
                    onDelete = { feel(null); onDelete(all = false) },
                    onClearAll = { feel(null); onDelete(all = true) },
                    onEnter = { feel(null); onEnter() },
                    modifier = keyboardModifier,
                    onLongKey = ::onLongPhoneKey,
                    navLabels = navLabels,
                    callLabel = soundLabel,
                )
            } else {
                Keypad(
                    onKey = ::onPhoneKey,
                    onChar = { feel(it); onChar(it) },
                    modifier = keyboardModifier,
                    onLongKey = ::onLongPhoneKey,
                    onLongChar = { feel(it); onLongChar(it) },
                    navLabels = navLabels,
                    callLabel = soundLabel,
                )
            }
        },
        modifier = Modifier.background(colors.body).windowInsetsPadding(WindowInsets.displayCutout),
        name = prefs.phoneName,
        showName = locked || screen.showsName,
        // Games get most of the screen; pinball and cards, played by touch, the most.
        keyboardWeight = when {
            locked -> 1.1f
            screen == Screen.Pinball || screen == Screen.Spider -> 0.45f
            screen.isGame -> 0.58f
            else -> 1.1f
        },
    ) { modifier ->
        val labels = if (inGame) emptyList() else softKeys.map { it.label to it.onClick }
        val (background, textColor) = when (screen) {
            Screen.Snake -> SnakeBackground to Color.White
            else -> colors.lcd to colors.lcdText
        }
        Lcd(
            labels, modifier, background = background, textColor = textColor,
            padding = if (inGame) 4.dp else 8.dp,
        ) {
            if (locked) {
                LockedContent(armed = menuPressedAt != 0L)
            } else when (screen) {
                Screen.LockSetup -> {
                    TitleBar(stringResource(R.string.lock_title))
                    LcdText(
                        stringResource(
                            if (LockService.isEnabled(context)) R.string.lock_ready else R.string.lock_explain,
                        ),
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp),
                        maxLines = Int.MAX_VALUE,
                    )
                }
                Screen.Home -> HomeContent(
                    dialed,
                    // Without access nothing is kept, even if the service has not stopped yet.
                    notices = if (noticeAccess) Notices.list else emptyList(),
                    onNotice = ::openNotice,
                    onDismissNotice = { NokiaNotificationListenerService.dismiss(it.key) },
                    onClearNotices = ::clearNotices,
                ) { go(Screen.Connections) }
                Screen.NoticeAsk -> {
                    TitleBar(stringResource(R.string.app_name))
                    LcdText(
                        stringResource(R.string.notices_question),
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp),
                        maxLines = Int.MAX_VALUE,
                    )
                }
                Screen.Notifications -> {
                    TitleBar(stringResource(R.string.notifications), Notices.list.size.toString())
                    NoticeList(
                        if (noticeAccess) Notices.list else emptyList(),
                        onOpen = ::openNotice,
                        onDismiss = { NokiaNotificationListenerService.dismiss(it.key) },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
                    )
                }
                Screen.NoticeAccess -> {
                    TitleBar(stringResource(R.string.notifications))
                    LcdText(
                        stringResource(if (noticeAccess) R.string.notices_ready else R.string.notices_explain),
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp),
                        maxLines = Int.MAX_VALUE,
                    )
                }
                Screen.Apps -> AppMenu(shown, search, pending, ::toggleFavorite, ::open)
                Screen.Thread -> ThreadContent(sms.second[threadAddress] ?: threadAddress, threadMessages)
                Screen.SendConfirm -> {
                    TitleBar(stringResource(R.string.soft_send))
                    LcdText(
                        stringResource(
                            R.string.send_confirm, recipient, SmsMessage.calculateLength(message, false)[0],
                        ),
                        Modifier.padding(top = 8.dp), maxLines = 6,
                    )
                }
                Screen.Arcade -> with(extra) {
                    Content(
                        record = prefs.record(id),
                        onRecord = { prefs.setRecord(id, it) },
                        )
                }
                Screen.Mines -> MinesContent(
                    mines,
                    onReveal = { mines = mines.reveal(it) },
                    onFlag = { mines = mines.flag(it) },
                    onReset = { mines = MinesState() },
                )
                Screen.Spider -> SpiderContent(
                    spider,
                    onTap = { column, index -> spider = spider.tap(column, index) },
                    onMove = { from, index, to -> spider = spider.move(from, index, to) },
                    onDeal = { spider = spider.deal() },
                    onUndo = { spider = spider.undo() },
                )
                Screen.Pinball -> PinballContent(pinball, prefs.pinballBest, flippers) {
                    prefs.pinballGame = pinball.encode()
                    if (pinball.score > prefs.pinballBest) prefs.pinballBest = pinball.score
                }
                Screen.NewContact -> {
                    TitleBar(stringResource(R.string.contact_new), if (!onNumber && !prefs.qwerty) mode.label else "")
                    listOf(
                        Triple(R.string.contact_name, contactName, false),
                        Triple(R.string.contact_number, contactNumber, true),
                    ).forEach { (label, value, isNumber) ->
                        LcdText(stringResource(label), Modifier.padding(top = 10.dp), small = true)
                        val box = Modifier.fillMaxWidth().border(1.dp, colors.lcdText)
                            .clickable {
                                onNumber = isNumber
                                lastTap = null
                                mode = if (isNumber) InputMode.Number else InputMode.Upper
                            }
                            .padding(6.dp)
                        if (onNumber == isNumber) {
                            CursorText(value, colors.lcdText, box, pending = pending && !isNumber, maxLines = 1)
                        } else {
                            LcdText(value, box)
                        }
                    }
                }
                Screen.Gallery -> if (context.granted(photoPermission)) {
                    GalleryGrid(photos) {
                        photoIndex = it
                        go(Screen.Photo)
                    }
                } else {
                    ListContent(
                        stringResource(R.string.menu_gallery),
                        listOf(Row(stringResource(R.string.photos_allow)) { ask(photoPermission) }),
                    )
                }
                Screen.Photo -> PhotoContent(photos, photoIndex)
                Screen.Contacts -> {
                    TitleBar(
                        stringResource(if (picking) R.string.recipient else R.string.contacts),
                        if (picking && !prefs.qwerty) mode.label else "",
                    )
                    CursorText(
                        stringResource(R.string.search_field, search), colors.lcdText,
                        Modifier.fillMaxWidth().padding(vertical = 6.dp), pending = pending,
                        fontSize = 14.sp, maxLines = 1,
                    )
                    ListRows(rows)
                }
                Screen.Message, Screen.Name -> {
                    val hint = if (prefs.qwerty) {
                        stringResource(R.string.editor_hint_qwerty)
                    } else {
                        stringResource(R.string.editor_hint)
                    }
                    val shownMode = if (prefs.qwerty) "" else mode.label
                    val underline = pending && mode != InputMode.Number
                    if (screen == Screen.Name) {
                        EditorContent(
                            stringResource(R.string.phone_name), nameDraft, hint, shownMode,
                            "${NAME_MAX - nameDraft.length}", underline,
                        )
                    } else {
                        EditorContent(
                            if (recipient.isEmpty()) stringResource(R.string.write_message)
                            else stringResource(R.string.message_to, recipient),
                            message, hint, shownMode,
                            // Characters left in this SMS / number of SMS, counted by Android.
                            SmsMessage.calculateLength(message, false).let { "${it[2]}/${it[0]}" },
                            underline,
                        )
                    }
                }
                Screen.Snake -> SnakeContent(
                    snake, prefs.snakeBest,
                    onTurn = { snake = snake.turn(it) },
                    onToggle = { softKeys[1].onClick() },
                    onTick = { snake = snake.step() },
                    onSave = { prefs.snakeGame = it.encode() },
                )
                Screen.Radio -> RadioContent(prefs.radioFrequency, radioOn)
                Screen.Scanner -> ScannerContent(scan, scannerOn) { scan = scan.step(Random) }
                Screen.Code -> {
                    TitleBar(shownCode.code)
                    LcdText(stringResource(shownCode.title), Modifier.padding(vertical = 8.dp), maxLines = 3)
                    LcdText(
                        shownCode.text(context),
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        maxLines = Int.MAX_VALUE,
                    )
                }
                Screen.Restore -> LcdText(stringResource(R.string.restore_confirm), maxLines = 4)
                Screen.About -> AboutContent(
                    remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() },
                )
                else -> ListContent(
                    if (screen == Screen.Contact) contact?.name.orEmpty() else stringResource(
                        when (screen) {
                            Screen.GoTo -> R.string.soft_go_to
                            Screen.Messages -> R.string.menu_messages
                            Screen.CallLog, Screen.Number -> R.string.menu_call_log
                            Screen.SpiderLevel -> R.string.spider
                            Screen.Games -> R.string.games
                            Screen.Theme -> R.string.phone_theme
                            Screen.Tones -> R.string.tones
                            Screen.KeySound -> R.string.keypad_tone
                            Screen.TonePick -> when (toneType) {
                                RingtoneManager.TYPE_NOTIFICATION -> R.string.tone_message
                                RingtoneManager.TYPE_ALARM -> R.string.tone_alarm
                                else -> R.string.tone_ring
                            }
                            Screen.Language -> R.string.language
                            Screen.Connections -> R.string.connections
                            Screen.Codes -> R.string.secret_codes
                            else -> R.string.soft_setting
                        },
                    ),
                    rows,
                )
            }
        }
    }
}
