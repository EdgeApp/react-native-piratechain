package app.edge.rnpiratechain;

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.Collections.emptyList


class RNPiratechainPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext) =
        listOf<NativeModule>(
            RNPiratechainModule(reactContext)
        )

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> =
        emptyList()
}
