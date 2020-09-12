package li.klass.photo_copy

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import li.klass.photo_copy.ui.main.MainFragment

class MediaBroadcastReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        context.sendBroadcast(Intent(MainFragment.RELOAD_SD_CARDS))
    }

}