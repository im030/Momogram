#!/bin/bash

set -e

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/wallet-engine

pushd TMessagesProj/jni/wallet-engine
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
popd

./TMessagesProj/jni/prebuild/build_wallet_engine.sh