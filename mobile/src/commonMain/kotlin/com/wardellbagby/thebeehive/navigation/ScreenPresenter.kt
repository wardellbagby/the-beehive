package com.wardellbagby.thebeehive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withCompositionLocals
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule

abstract class ScreenPresenter<PropsT, OutputT, RenderT> {
  @Composable protected abstract fun present(props: PropsT, onOutput: (OutputT) -> Unit): RenderT

  @Composable
  fun render(props: PropsT, onOutput: (OutputT) -> Unit): RenderT {
    // If we're already in Molecule land, no need to re-launch as a Molecule; it can simply
    // run the new Presenter directly.
    if (!LocalIsMolecule.current) {
      error(
        "Cannot render Presenter without there being a root presenter higher in the tree. " +
          "Did you mean to call renderRoot?"
      )
    }

    return present(props, onOutput)
  }
}

@Composable
fun <PropsT, OutputT, RenderT> ScreenPresenter<PropsT, OutputT, RenderT>.renderRoot(
  vararg locals: ProvidedValue<*>,
  props: PropsT,
  onOutput: (OutputT) -> Unit,
): RenderT {
  if (LocalIsMolecule.current) {
    error(
      "No need to use renderRoot as we've rendered a root presenter higher in the tree. Did you mean to call render?"
    )
  }

  val scope = rememberCoroutineScope()
  val moleculeContext = createDefaultMoleculeContext()

  return remember {
      // todo can probably not use the Context here and instead call
      // withDefaultPlatformCompositionLocals in Compose UI, have it return an array of
      // ProvidedValues, and then just pass that into Molecule land normally...
      scope.launchMolecule(context = moleculeContext, mode = ContextClock) {
        withCompositionLocals(LocalIsMolecule provides true, *locals) {
          withDefaultPlatformCompositionLocals(moleculeContext) { render(props, onOutput) }
        }
      }
    }
    .collectAsStateWithLifecycle()
    .value
}

abstract class BasicScreenPresenter<RenderT> : ScreenPresenter<Unit, Unit, RenderT>() {
  @Composable
  final override fun present(props: Unit, onOutput: (Unit) -> Unit): RenderT {
    return present()
  }

  @Composable protected abstract fun present(): RenderT
}

@Composable fun <RenderT> BasicScreenPresenter<RenderT>.render(): RenderT = render(Unit) {}

@Composable
fun <OutputT, RenderT> ScreenPresenter<Unit, OutputT, RenderT>.render(
  onOutput: (OutputT) -> Unit
): RenderT = render(props = Unit, onOutput = onOutput)

private val LocalIsMolecule = staticCompositionLocalOf { false }
