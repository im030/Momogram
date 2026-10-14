#!/bin/bash

source "bin/init/env.sh"

git submodule update --init TMessagesProj/jni/third_party/libprisma


./TMessagesProj/jni/prebuild/build_prisma.sh