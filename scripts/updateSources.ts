// Run this script as `node -r sucrase/register ./scripts/updateSources.ts`
//
// It will download third-party source code, modify it,
// and install it into the correct locations.

import { execSync } from 'child_process'
import { deepList, justFiles, makeNodeDisklet, navigateDisklet } from 'disklet'
import { existsSync, mkdirSync } from 'fs'
import { join } from 'path'

import { copyCheckpoints } from './copyCheckpoints'

const disklet = makeNodeDisklet(join(__dirname, '../'))
const tmp = join(__dirname, '../tmp')

async function main(): Promise<void> {
  if (!existsSync(tmp)) mkdirSync(tmp)
  await downloadSources()
  await rebuildXcframework()
  await copySwift()
  await copyCheckpoints(disklet)
}

function downloadSources(): void {
  getRepo(
    'PirateLightClientKit',
    'https://github.com/PirateNetwork/PirateLightClientKit.git',
    // 0.22.0-beta:
    'e98ba08515e09cdd7221ac875226a8c4565dbdc5'
  )
  getRepo(
    'pirate-light-client-ffi',
    'https://github.com/PirateNetwork/pirate-light-client-ffi.git',
    // 0.3.1:
    'b07e6fb619a8f0eadd344f4e0d3e81d8a7a0a33a'
  )
}

/**
 * Re-packages pirate-light-client-ffi.
 *
 * An XCFramework can either include a static library (.a)
 * or a dynamically-linked library (.framework).
 * The pirate-light-client-ffi package tries to stuff a static library
 * into a dynamic framework, which doesn't work correctly.
 * We fix this by simply re-building the XCFramework.
 */
async function rebuildXcframework(): Promise<void> {
  console.log('Creating XCFramework...')
  await disklet.delete('ios/libpiratelc.xcframework')

  // Extract the static libraries:
  await disklet.setData(
    'tmp/lib/ios-simulator/libpiratelc.a',
    await disklet.getData(
      'tmp/pirate-light-client-ffi/releases/XCFramework/libpiratelc.xcframework/ios-arm64_x86_64-simulator/libpiratelc.framework/libpiratelc'
    )
  )
  await disklet.setData(
    'tmp/lib/ios/libpiratelc.a',
    await disklet.getData(
      'tmp/pirate-light-client-ffi/releases/XCFramework/libpiratelc.xcframework/ios-arm64/libpiratelc.framework/libpiratelc'
    )
  )

  // Build the XCFramework:
  loudExec(tmp, [
    'xcodebuild',
    '-create-xcframework',
    '-library',
    join(__dirname, '../tmp/lib/ios-simulator/libpiratelc.a'),
    '-library',
    join(__dirname, '../tmp/lib/ios/libpiratelc.a'),
    '-output',
    join(__dirname, '../ios/libpiratelc.xcframework')
  ])
}

/**
 * Copies swift code, with modifications.
 */
async function copySwift(): Promise<void> {
  console.log('Copying swift sources...')
  const fromDisklet = navigateDisklet(
    disklet,
    'tmp/PirateLightClientKit/Sources'
  )
  const toDisklet = navigateDisklet(disklet, 'ios')
  await toDisklet.delete('PirateLightClientKit/')
  const files = justFiles(await deepList(fromDisklet, 'PirateLightClientKit/'))

  for (const file of files) {
    const text = await fromDisklet.getText(file)
    const fixed = text
      // We are lumping everything into one module,
      // so we don't need to import this externally:
      .replace('import libpiratelc', '')
      // This is ambiguous with the Swift one:
      .replace(/Expression</g, 'SQLite.Expression<')
      // The Swift package manager synthesizes a "Bundle.module" accessor,
      // but with CocoaPods we need to load things manually:
      .replace(
        'Bundle.module.bundleURL.appendingPathComponent("piratesaplingtree-checkpoints/mainnet/")',
        'Bundle.main.url(forResource: "piratechain-mainnet", withExtension: "bundle")!'
      )
      .replace(
        'Bundle.module.bundleURL.appendingPathComponent("piratesaplingtree-checkpoints/testnet/")',
        'Bundle.main.url(forResource: "piratechain-testnet", withExtension: "bundle")!'
      )
      .replace(/static let macOS = BundleCheckpointURLProvider.*}\)/s, '')

    await toDisklet.setText(file, fixed)
  }

  // Copy the Rust header into the Swift location:
  await disklet.setText(
    'ios/libpiratelc.h',
    await disklet.getText(
      'tmp/pirate-light-client-ffi/releases/XCFramework/libpiratelc.xcframework/ios-arm64/libpiratelc.framework/Headers/piratelc.h'
    )
  )
}

/**
 * Clones a git repo and checks our a hash.
 */
function getRepo(name: string, uri: string, hash: string): void {
  const path = join(tmp, name)

  // Either clone or fetch:
  if (!existsSync(path)) {
    console.log(`Cloning ${name}...`)
    loudExec(tmp, ['git', 'clone', uri, name])
  } else {
    // We might already have the right commit, so fetch lazily:
    try {
      loudExec(path, ['git', 'fetch'])
    } catch (error) {
      console.log(error)
    }
  }

  // Checkout:
  console.log(`Checking out ${name}...`)
  execSync(`git checkout -f ${hash}`, {
    cwd: path,
    stdio: 'inherit',
    encoding: 'utf8'
  })
}

/**
 * Runs a command and displays its results.
 */
function loudExec(path: string, argv: string[]): void {
  execSync(argv.join(' '), {
    cwd: path,
    stdio: 'inherit',
    encoding: 'utf8'
  })
}

main().catch(error => {
  console.log(error)
  process.exit(1)
})
