#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/third_party/xiph/ogg
git submodule update --init TMessagesProj/jni/third_party/xiph/opus
git submodule update --init TMessagesProj/jni/third_party/xiph/opusfile

./TMessagesProj/jni/prebuild/build_opus.sh
