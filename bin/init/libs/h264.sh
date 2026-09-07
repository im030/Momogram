#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/third_party/openh264
TMessagesProj/jni/prebuild/build_openh264.sh || exit 1
