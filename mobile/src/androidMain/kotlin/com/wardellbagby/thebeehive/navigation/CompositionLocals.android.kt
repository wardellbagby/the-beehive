package com.wardellbagby.thebeehive.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocals
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class AndroidContext(val context: Context) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AndroidContext>
}

class AndroidLifecycleOwner(val owner: LifecycleOwner) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AndroidLifecycleOwner>
}

@Composable
actual fun <T> withDefaultPlatformCompositionLocals(
    coroutineContext: CoroutineContext,
    body: @Composable (() -> T)
): T {
    return withCompositionLocals(
        LocalContext provides coroutineContext[AndroidContext]!!.context,
        LocalLifecycleOwner provides coroutineContext[AndroidLifecycleOwner]!!.owner
    ) {
        body()
    }
}

@Composable
actual fun createDefaultMoleculeContext(): CoroutineContext {
    return EmptyCoroutineContext +
            AndroidContext(LocalContext.current) +
            AndroidLifecycleOwner(LocalLifecycleOwner.current)
}
