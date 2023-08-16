#!/usr/bin/env node

// Checkpoints are committed into the appropriate Android directory but need to be copied to iOS.
// This script assumes it is run from within the app directory like so
//
// node ./node_modules/react-native-piratechain/scripts/copyCheckpoints.js

// eslint-disable-next-line no-undef
const { execSync } = require('child_process')

const cmd = `cp -r node_modules/react-native-piratechain/android/src/main/assets/piratesaplingtree/mainnet/* ios/Pods/PirateLightClientKit/Sources/PirateLightClientKit/Resources/piratesaplingtree-checkpoints/mainnet/`
execSync(cmd)
