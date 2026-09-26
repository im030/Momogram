#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/third_party/wamr


./TMessagesProj/jni/prebuild/build_wamr.sh