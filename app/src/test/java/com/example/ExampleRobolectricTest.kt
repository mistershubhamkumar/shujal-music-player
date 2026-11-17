package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesRepository
import com.example.model.RepeatModeState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Shujal Music", appName)
    }

    @Test
    fun `shuffle and repeat modes persist across PreferencesRepository instances`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = PreferencesRepository(context)
        repo1.setShuffleEnabled(true)
        repo1.setRepeatMode(RepeatModeState.ONE)

        val repo2 = PreferencesRepository(context)
        val restored = repo2.preferencesFlow.first()
        assertTrue(restored.shuffleEnabled)
        assertEquals(RepeatModeState.ONE, restored.repeatMode)

        repo2.setRepeatMode(RepeatModeState.ALL)
        repo2.setShuffleEnabled(false)
        val restoredSecond = PreferencesRepository(context).preferencesFlow.first()
        assertEquals(RepeatModeState.ALL, restoredSecond.repeatMode)
        assertEquals(false, restoredSecond.shuffleEnabled)
    }
}
