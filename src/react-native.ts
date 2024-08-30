import {
  EventSubscription,
  NativeEventEmitter,
  NativeModules
} from 'react-native'

import {
  Addresses,
  BlockRange,
  ConfirmedTransaction,
  InitializerConfig,
  Network,
  SpendFailure,
  SpendInfo,
  SpendSuccess,
  SynchronizerCallbacks,
  WalletBalance
} from './types'
export * from './types'

const { RNPiratechain } = NativeModules

type Callback = (...args: any[]) => any

export const Tools = {
  deriveViewingKey: async (
    seedBytesHex: string,
    network: Network
  ): Promise<string> => {
    const result = await RNPiratechain.deriveViewingKey(seedBytesHex, network)
    return result
  },
  getBirthdayHeight: async (host: string, port: number): Promise<number> => {
    const result = await RNPiratechain.getBirthdayHeight(host, port)
    return result
  },
  isValidAddress: async (
    address: string,
    network: Network = 'mainnet'
  ): Promise<boolean> => {
    const result = await RNPiratechain.isValidAddress(address, network)
    return result
  }
}

export class Synchronizer {
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

  async stop(): Promise<String> {
    this.unsubscribe()
    const result = await RNPiratechain.stop(this.alias)
    return result
  }

  async initialize(initializerConfig: InitializerConfig): Promise<void> {
    await RNPiratechain.initialize(
      initializerConfig.mnemonicSeed,
      initializerConfig.birthdayHeight,
      initializerConfig.alias,
      initializerConfig.networkName,
      initializerConfig.defaultHost,
      initializerConfig.defaultPort
    )
  }

  async deriveUnifiedAddress(): Promise<Addresses> {
    const result = await RNPiratechain.deriveUnifiedAddress(this.alias)
    return result
  }

  async getLatestNetworkHeight(alias: string): Promise<number> {
    const result = await RNPiratechain.getLatestNetworkHeight(alias)
    return result
  }

  async getBalance(): Promise<WalletBalance> {
    const result = await RNPiratechain.getBalance(this.alias)
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

  rescan(): void {
    RNPiratechain.rescan(this.alias)
  }

  async sendToAddress(
    spendInfo: SpendInfo
  ): Promise<SpendSuccess | SpendFailure> {
    const result = await RNPiratechain.sendToAddress(
      this.alias,
      spendInfo.zatoshi,
      spendInfo.toAddress,
      spendInfo.memo,
      spendInfo.mnemonicSeed
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
