package li.klass.photo_copy

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import li.klass.photo_copy.ui.main.MainFragment
import li.klass.photo_copy.ui.main.MainFragment.Companion.RELOAD_SD_CARDS
import li.klass.photo_copy.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        if (savedInstanceState == null) {
            loadMainFragment()
        }

        requestPermissions(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1
        )
    }

    private fun loadMainFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commitNow()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_refresh) {
            sendBroadcast(Intent(RELOAD_SD_CARDS))
            return true
        }
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        if (item.itemId == R.id.action_openwifisettings) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS));
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
