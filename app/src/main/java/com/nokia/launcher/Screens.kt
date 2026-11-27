package com.nokia.launcher

/** Every screen of the launcher and where Back leads from it. */
internal enum class Screen(val parent: () -> Screen = { Home }) {
    Home,
    NoticeAsk,
    NoticeAccess,
    Notifications({ GoTo }),
    GoTo,
    Apps,
    Messages({ GoTo }),
    Thread({ Messages }),
    Message({ Messages }),
    SendConfirm({ Message }),
    Contacts({ GoTo }),
    Contact({ Contacts }),
    NewContact({ Contacts }),
    CallLog({ GoTo }),
    Number({ CallLog }),
    Gallery({ GoTo }),
    Photo({ Gallery }),
    Games({ GoTo }),
    Snake({ Games }),
    Pinball({ Games }),
    Spider({ Games }),
    SpiderLevel({ Spider }),
    Mines({ Games }),
    Arcade({ Games }),
    Settings,
    Name({ Settings }),
    Theme({ Settings }),
    Tones({ Settings }),
    TonePick({ Tones }),
    KeySound({ Tones }),
    Language({ Settings }),
    Restore({ Settings }),
    About({ Settings }),
    Radio({ GoTo }),
    Scanner,
    LockSetup({ Apps }),
    Connections({ GoTo }),
    Codes({ GoTo }),
    Code;

    /** Games draw their own picture, so they get a shorter keyboard. */
    val isGame get() = this == Snake || this == Pinball || this == Spider || this == Mines || this == Arcade

    /** Only the phone's own screens show the wordmark; built-in apps use the whole display. */
    val showsName get() = this == Home || this == NoticeAsk || this == GoTo || this == Settings || parent() == Settings
}
