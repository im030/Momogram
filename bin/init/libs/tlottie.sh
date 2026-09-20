#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/tlottie_lib


./TMessagesProj/jni/prebuild/scripts/tlottie/host.sh

mkdir -p TMessagesProj/jni/prebuild/lib/{x86,x86_64,arm64-v8a,armeabi-v7a}
mv -fv ./TMessagesProj/jni/prebuild/tlottie-build/i686-linux-android/release-nostd/libtlottie.a ./TMessagesProj/jni/prebuild/lib/x86/
mv -fv ./TMessagesProj/jni/prebuild/tlottie-build/x86_64-linux-android/release-nostd/libtlottie.a ./TMessagesProj/jni/prebuild/lib/x86_64/
mv -fv ./TMessagesProj/jni/prebuild/tlottie-build/aarch64-linux-android/release-nostd/libtlottie.a ./TMessagesProj/jni/prebuild/lib/arm64-v8a/
mv -fv ./TMessagesProj/jni/prebuild/tlottie-build/armv7-linux-androideabi/release-nostd/libtlottie.a ./TMessagesProj/jni/prebuild/lib/armeabi-v7a/