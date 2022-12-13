import {
  EventSubscription,
  NativeEventEmitter,
  NativeModules
} from 'react-native'

import {
  BlockRange,
  ConfirmedTransaction,
  InitializerConfig,
  Network,
  PendingTransaction,
  SpendInfo,
  SynchronizerCallbacks,
  UnifiedViewingKey,
  WalletBalance
} from './types'

const { RNPiratechain } = NativeModules

type Callback = (...args: any[]) => any

export const KeyTool = {
  deriveViewingKey: async (
    seedBytesHex: string,
    network: Network
  ): Promise<UnifiedViewingKey> => {
    const result = await RNPiratechain.deriveViewingKey(seedBytesHex, network)
    return result
  },
  deriveSpendingKey: async (
    seedBytesHex: string,
    network: Network
  ): Promise<string> => {
    const result = await RNPiratechain.deriveSpendingKey(seedBytesHex, network)
    return result
  }
}

export const AddressTool = {
  deriveShieldedAddress: async (
    viewingKey: string,
    network: Network
  ): Promise<string> => {
    const result = await RNPiratechain.deriveShieldedAddress(
      viewingKey,
      network
    )
    return result
  },
  isValidShieldedAddress: async (
    address: string,
    network: Network = 'mainnet'
  ): Promise<boolean> => {
    const result = await RNPiratechain.isValidShieldedAddress(address, network)
    return result
  },
  isValidTransparentAddress: async (
    address: string,
    network: Network = 'mainnet'
  ): Promise<boolean> => {
    const result = await RNPiratechain.isValidTransparentAddress(
      address,
      network
    )
    return result
  }
}

class Synchronizer {
  eventEmitter: NativeEventEmitter
  subscriptions: EventSubscription[]
  alias: string
  network: Network

  constructor(alias: string, network: Network) {
    this.eventEmitter = new NativeEventEmitter(RNPiratechain)
    this.subscriptions = []
    this.alias = alias
    this.network = network
  }

  async start(): Promise<String> {
    const result = await RNPiratechain.start(this.alias)
    return result
  }

  async stop(): Promise<String> {
    this.unsubscribe()
    const result = await RNPiratechain.stop(this.alias)
    return result
  }

  async initialize(initializerConfig: InitializerConfig): Promise<void> {
    await RNPiratechain.initialize(
      initializerConfig.fullViewingKey.extfvk,
      initializerConfig.fullViewingKey.extpub,
      initializerConfig.birthdayHeight,
      initializerConfig.alias,
      initializerConfig.networkName,
      initializerConfig.defaultHost,
      initializerConfig.defaultPort
    )
  }

  async getLatestNetworkHeight(alias: string): Promise<number> {
    const result = await RNPiratechain.getLatestNetworkHeight(alias)
    return result
  }

  async getShieldedBalance(): Promise<WalletBalance> {
    const result = await RNPiratechain.getShieldedBalance(this.alias)
    return result
  }

  async getTransactions(range: BlockRange): Promise<ConfirmedTransaction[]> {
    const result = await RNPiratechain.getTransactions(
      this.alias,
      range.first,
      range.last
    )
    return result
  }

  rescan(height: number): void {
    RNPiratechain.rescan(this.alias, height)
  }

  async sendToAddress(spendInfo: SpendInfo): Promise<PendingTransaction> {
    const result = await RNPiratechain.spendToAddress(
      this.alias,
      spendInfo.zatoshi,
      spendInfo.toAddress,
      spendInfo.memo,
      spendInfo.fromAccountIndex,
      spendInfo.spendingKey
    )
    return result
  }

  // Events

  subscribe({ onStatusChanged, onUpdate }: SynchronizerCallbacks): void {
    this.setListener('StatusEvent', onStatusChanged)
    this.setListener('UpdateEvent', onUpdate)
  }

  private setListener<T>(
    eventName: string,
    callback: Callback = (t: any) => null
  ): void {
    this.subscriptions.push(
      this.eventEmitter.addListener(eventName, arg =>
        arg.alias === this.alias ? callback(arg) : null
      )
    )
  }

  unsubscribe(): void {
    this.subscriptions.forEach(subscription => {
      subscription.remove()
    })
  }
}

export const makeSynchronizer = async (
  initializerConfig: InitializerConfig
): Promise<Synchronizer> => {
  const synchronizer = new Synchronizer(
    initializerConfig.alias,
    initializerConfig.networkName
  )
  await synchronizer.initialize(initializerConfig)
  return synchronizer
}
