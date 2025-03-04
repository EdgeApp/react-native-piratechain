package app.edge.rnpiratechain

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pirate.android.sdk.SdkSynchronizer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.exception.LightWalletException
import pirate.android.sdk.ext.*
import pirate.android.sdk.internal.*
import pirate.android.sdk.model.*
import pirate.android.sdk.tool.DerivationTool
import pirate.android.sdk.type.*
import pirate.lightwallet.client.LightWalletClient
import pirate.lightwallet.client.model.LightWalletEndpoint
import pirate.lightwallet.client.model.Response
import pirate.lightwallet.client.new
import kotlin.coroutines.EmptyCoroutineContext

class RNPiratechainModule(
    private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {
    /**
     * Scope for anything that out-lives the synchronizer, meaning anything that can be used before
     * the synchronizer starts or after it stops. Everything else falls within the scope of the
     * synchronizer and should use `synchronizer.coroutineScope` whenever a scope is needed.
     *
     * In a real production app, we'd use the scope of the parent activity
     */
    var moduleScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
    var synchronizerMap = mutableMapOf<String, SdkSynchronizer>()

    val networks = mapOf("mainnet" to PirateNetwork.Mainnet, "testnet" to PirateNetwork.Testnet)

    override fun getName() = "RNPiratechain"

    @ReactMethod
    fun initialize(
        seed: String,
        birthdayHeight: Int,
        alias: String,
        networkName: String = "mainnet",
        defaultHost: String = "mainnet.lightwalletd.com",
        defaultPort: Int = 9067,
        promise: Promise,
    ) = moduleScope.launch {
        promise.wrap {
            var network = networks.getOrDefault(networkName, PirateNetwork.Mainnet)
            var endpoint = LightWalletEndpoint(defaultHost, defaultPort, true)
            var seedPhrase = SeedPhrase.new(seed)
            if (!synchronizerMap.containsKey(alias)) {
                synchronizerMap.set(
                    alias,
                    Synchronizer.new(
                        reactApplicationContext,
                        network,
                        alias,
                        endpoint,
                        seedPhrase.toByteArray(),
                        BlockHeight.new(network, birthdayHeight.toLong()),
                    ) as SdkSynchronizer,
                )
            }
            val wallet = getWallet(alias)
            val scope = wallet.coroutineScope
            wallet.processorInfo.collectWith(scope, { update ->
                scope.launch {
                    var lastDownloadedHeight = this.async { wallet.processor.downloader.getLastDownloadedHeight() }.await()
                    if (lastDownloadedHeight == null) lastDownloadedHeight = BlockHeight.new(wallet.network, birthdayHeight.toLong())

                    var lastScannedHeight = update.lastSyncedHeight
                    if (lastScannedHeight == null) lastScannedHeight = BlockHeight.new(wallet.network, birthdayHeight.toLong())

                    var networkBlockHeight = update.networkBlockHeight
                    if (networkBlockHeight == null) networkBlockHeight = BlockHeight.new(wallet.network, birthdayHeight.toLong())

                    sendEvent("UpdateEvent") { args ->
                        args.putString("alias", alias)
                        args.putInt("lastDownloadedHeight", lastDownloadedHeight.value.toInt())
                        args.putInt("lastScannedHeight", lastScannedHeight.value.toInt())
                        args.putInt(
                            "scanProgress",
                            wallet.processor.progress.value
                                .toPercentage(),
                        )
                        args.putInt("networkBlockHeight", networkBlockHeight.value.toInt())
                    }
                }
            })
            wallet.status.collectWith(scope, { status ->
                sendEvent("StatusEvent") { args ->
                    args.putString("alias", alias)
                    args.putString("name", status.toString())
                }
            })

            fun handleError(
                level: String,
                error: Throwable?,
            ) {
                sendEvent("ErrorEvent") { args ->
                    args.putString("alias", alias)
                    args.putString("level", level)
                    args.putString("message", error?.message ?: "Unknown error")
                }
            }

            // Error listeners
            wallet.onCriticalErrorHandler = { error ->
                handleError("critical", error)
                false
            }
            wallet.onProcessorErrorHandler = { error ->
                handleError("error", error)
                true
            }
            wallet.onSetupErrorHandler = { error ->
                handleError("error", error)
                false
            }
            wallet.onSubmissionErrorHandler = { error ->
                handleError("error", error)
                false
            }
            wallet.onChainErrorHandler = { errorHeight, rewindHeight ->
                val message = "Chain error detected at height: $errorHeight. Rewinding to: $rewindHeight"
                handleError("error", Throwable(message))
            }
            return@wrap null
        }
    }

    @ReactMethod
    fun stop(
        alias: String,
        promise: Promise,
    ) {
        promise.wrap {
            val wallet = getWallet(alias)
            wallet.close()
            synchronizerMap.remove(alias)
            return@wrap null
        }
    }

    fun inRange(
        tx: TransactionOverview,
        first: Int,
        last: Int,
    ): Boolean {
        if (tx.minedHeight != null && tx.minedHeight!!.value >= first.toLong() && tx.minedHeight!!.value <= last.toLong()) {
            return true
        }
        return false
    }

    suspend fun collectTxs(
        wallet: SdkSynchronizer,
        limit: Int,
    ): List<TransactionOverview> {
        val allTxs = mutableListOf<TransactionOverview>()
        val job =
            wallet.coroutineScope.launch {
                wallet.transactions.collect { txList ->
                    txList.forEach { tx ->
                        allTxs.add(tx)
                    }
                    if (allTxs.size == limit) {
                        cancel()
                    }
                }
            }
        job.join()

        return allTxs
    }

    suspend fun parseTx(
        wallet: SdkSynchronizer,
        tx: TransactionOverview,
    ): WritableMap {
        val map = Arguments.createMap()
        val job =
            wallet.coroutineScope.launch {
                map.putString("value", tx.netValue.value.toString())
                map.putInt("minedHeight", tx.minedHeight!!.value.toInt())
                map.putInt("blockTimeInSeconds", tx.blockTimeEpochSeconds.toInt())
                map.putString("rawTransactionId", tx.rawId.byteArray.toHexReversed())
                if (tx.isSentTransaction) {
                    val recipient = wallet.getRecipients(tx).first()
                    if (recipient is TransactionRecipient.Address) {
                        map.putString("toAddress", recipient.addressValue)
                    }
                }
                if (tx.memoCount > 0) {
                    try {
                        val memos = wallet.getMemos(tx).take(tx.memoCount).toList()
                        map.putArray("memos", Arguments.fromList(memos))
                    } catch (t: Throwable) {
                        // We can ignore these errors
                        map.putArray("memos", Arguments.createArray())
                    }
                } else {
                    map.putArray("memos", Arguments.createArray())
                }
            }
        job.join()
        return map
    }

    @ReactMethod
    fun getTransactions(
        alias: String,
        first: Int,
        last: Int,
        promise: Promise,
    ) {
        val wallet = getWallet(alias)

        wallet.coroutineScope.launch {
            promise.wrap {
                val numTxs = async { wallet.getTransactionCount() }.await()
                val nativeArray = Arguments.createArray()
                if (numTxs == 0) {
                    promise.resolve(nativeArray)
                    return@launch
                }

                val allTxs = async { collectTxs(wallet, numTxs) }.await()
                val filteredTxs = allTxs.filter { tx -> inRange(tx, first, last) }

                filteredTxs
                    .map { tx ->
                        launch {
                            val parsedTx = parseTx(wallet, tx)
                            nativeArray.pushMap(parsedTx)
                        }
                    }.forEach { it.join() }

                return@wrap nativeArray
            }
        }
    }

    @ReactMethod
    fun rescan(
        alias: String,
        promise: Promise,
    ) {
        val wallet = getWallet(alias)
        wallet.coroutineScope.launch {
            promise.wrap {
                wallet.coroutineScope.async { wallet.rewindToNearestHeight(wallet.latestBirthdayHeight, true) }.await()
                return@wrap null
            }
        }
    }

    @ReactMethod
    fun deriveViewingKey(
        seed: String,
        network: String = "mainnet",
        promise: Promise,
    ) {
        var seedPhrase = SeedPhrase.new(seed)
        moduleScope.launch {
            promise.wrap {
                var keys =
                    moduleScope
                        .async {
                            DerivationTool.getInstance().deriveUnifiedFullViewingKeys(
                                seedPhrase.toByteArray(),
                                networks.getOrDefault(network, PirateNetwork.Mainnet),
                                DerivationTool.DEFAULT_NUMBER_OF_ACCOUNTS,
                            )[0]
                        }.await()
                return@wrap keys.encoding
            }
        }
    }

    //
    // Properties
    //

    @ReactMethod
    fun getLatestNetworkHeight(
        alias: String,
        promise: Promise,
    ) = promise.wrap {
        val wallet = getWallet(alias)
        return@wrap wallet.latestHeight
    }

    @ReactMethod
    fun getBirthdayHeight(
        host: String,
        port: Int,
        promise: Promise,
    ) {
        moduleScope.launch {
            promise.wrap {
                var endpoint = LightWalletEndpoint(host, port, true)
                var lightwalletService = LightWalletClient.new(reactApplicationContext, endpoint)
                return@wrap when (val response = lightwalletService.getLatestBlockHeight()) {
                    is Response.Success -> {
                        response.result.value.toInt()
                    }

                    is Response.Failure -> {
                        throw LightWalletException.DownloadBlockException(
                            response.code,
                            response.description,
                            response.toThrowable(),
                        )
                    }
                }
            }
        }
    }

    @ReactMethod
    fun getBalance(
        alias: String,
        promise: Promise,
    ) {
        val wallet = getWallet(alias)
        var availableZatoshi = Arrrtoshi(0L)
        var totalZatoshi = Arrrtoshi(0L)

        wallet.coroutineScope.launch {
            promise.wrap {
                wallet.coroutineScope.async { wallet.refreshAllBalances() }.await()

                val transparentBalances = wallet.transparentBalances.value
                availableZatoshi = availableZatoshi.plus(transparentBalances?.available ?: Arrrtoshi(0L))
                totalZatoshi = totalZatoshi.plus(transparentBalances?.total ?: Arrrtoshi(0L))
                val saplingBalances = wallet.saplingBalances.value
                availableZatoshi = availableZatoshi.plus(saplingBalances?.available ?: Arrrtoshi(0L))
                totalZatoshi = totalZatoshi.plus(saplingBalances?.total ?: Arrrtoshi(0L))
                val orchardBalances = wallet.orchardBalances.value
                availableZatoshi = availableZatoshi.plus(orchardBalances?.available ?: Arrrtoshi(0L))
                totalZatoshi = totalZatoshi.plus(orchardBalances?.total ?: Arrrtoshi(0L))

                val map = Arguments.createMap()
                map.putString("totalZatoshi", totalZatoshi.value.toString())
                map.putString("availableZatoshi", availableZatoshi.value.toString())
                return@wrap map
            }
        }
    }

    @ReactMethod
    fun sendToAddress(
        alias: String,
        zatoshi: String,
        toAddress: String,
        memo: String = "",
        seed: String,
        promise: Promise,
    ) {
        val wallet = getWallet(alias)
        wallet.coroutineScope.launch {
            try {
                var seedPhrase = SeedPhrase.new(seed)
                val usk =
                    wallet.coroutineScope
                        .async {
                            DerivationTool.getInstance().deriveUnifiedSpendingKey(seedPhrase.toByteArray(), wallet.network, Account.DEFAULT)
                        }.await()
                val internalId =
                    wallet.sendToAddress(
                        usk,
                        Arrrtoshi(zatoshi.toLong()),
                        toAddress,
                        memo,
                    )
                val tx =
                    wallet.coroutineScope
                        .async {
                            wallet.transactions.flatMapConcat { list ->
                                flowOf(*list.toTypedArray())
                            }
                        }.await()
                        .firstOrNull { item -> item.id == internalId }
                if (tx == null) throw Exception("transaction failed")

                val map = Arguments.createMap()
                map.putString("txId", tx.rawId.byteArray.toHexReversed())
                if (tx.raw != null) map.putString("raw", tx.raw?.byteArray?.toHex())
                promise.resolve(map)
            } catch (t: Throwable) {
                promise.reject("Err", t)
            }
        }
    }

    //
    // AddressTool
    //

    @ReactMethod
    fun deriveUnifiedAddress(
        alias: String,
        promise: Promise,
    ) {
        val wallet = getWallet(alias)
        wallet.coroutineScope.launch {
            promise.wrap {
                // var unifiedAddress = wallet.coroutineScope.async { wallet.getUnifiedAddress(Account(0)) }.await()
                val saplingAddress = wallet.coroutineScope.async { wallet.getSaplingAddress(Account(0)) }.await()
                // val transparentAddress = wallet.coroutineScope.async { wallet.getTransparentAddress(Account(0)) }.await()

                val map = Arguments.createMap()
                // map.putString("unifiedAddress", unifiedAddress)
                map.putString("saplingAddress", saplingAddress)
                // map.putString("transparentAddress", transparentAddress)
                return@wrap map
            }
        }
    }

    @ReactMethod
    fun isValidAddress(
        address: String,
        network: String,
        promise: Promise,
    ) {
        moduleScope.launch {
            promise.wrap {
                var isValid = false
                val wallets = synchronizerMap.asIterable()
                for (wallet in wallets) {
                    if (wallet.value.network.networkName == network) {
                        isValid = wallet.value.isValidAddress(address)
                        break
                    }
                }
                return@wrap isValid
            }
        }
    }

    //
    // Utilities
    //

    /**
     * Retrieve wallet object from synchronizer map
     */
    private fun getWallet(alias: String): SdkSynchronizer {
        val wallet = synchronizerMap.get(alias)
        if (wallet == null) throw Exception("Wallet not found")
        return wallet
    }

    /**
     * Wrap the given block of logic in a promise, rejecting for any error.
     */
    private inline fun <T> Promise.wrap(block: () -> T) {
        try {
            resolve(block())
        } catch (t: Throwable) {
            reject("Err", t)
        }
    }

    private fun sendEvent(
        eventName: String,
        putArgs: (WritableMap) -> Unit,
    ) {
        val args = Arguments.createMap()
        putArgs(args)
        reactApplicationContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit(eventName, args)
    }

    inline fun ByteArray.toHexReversed(): String {
        val sb = StringBuilder(size * 2)
        var i = size - 1
        while (i >= 0) {
            sb.append(String.format("%02x", this[i--]))
        }
        return sb.toString()
    }
}
