#!/bin/bash

set -eu

declare -r app_filename="$([ -n "${BASH_SOURCE}" ] && realpath "${BASH_SOURCE[0]}" || realpath "${0}")"
declare -r app_directory="$(dirname "${app_filename}")"

declare -r source_directory="${app_directory}/td"
declare -r build_directory="${app_directory}/td/build"
declare -r install_directory="${app_directory}/td/build/install"

declare -ra architectures=(
	'armeabi-v7a'
	'arm64-v8a'
	'x86'
	'x86_64'
)

function checkPreRequisites {

	if ! [ -d "${source_directory}" ] || ! [ "$(ls -A "${source_directory}")" ]; then
		echo -e "\033[31mFailed! Submodule 'td' not found!\033[0m"
		echo -e "\033[31mTry to run: 'git submodule init && git submodule update'\033[0m"
		exit 1
	fi

	if [ -z "${NDK:-}" ]; then
		echo -e "\033[31mFailed! NDK is empty. Run 'export NDK=[PATH_TO_NDK]'\033[0m"
		exit 1
	fi
}

checkPreRequisites

cd "${app_directory}"

rm --force --recursive "${build_directory}"

cmake \
	-B "${build_directory}" \
	-S "${source_directory}" \
	-DTD_E2E_ONLY=ON

cmake \
	--build "${build_directory}" \
	--target 'prepare_cross_compiling'

for arch in "${architectures[@]}"; do
	rm --force --recursive "${build_directory}"

	echo "Building ${arch}..."

	cmake \
		-D CMAKE_TOOLCHAIN_FILE="${NDK}/build/cmake/android.toolchain.cmake" \
		-D ANDROID_ABI="${arch}" \
		-D ANDROID_PLATFORM='android-21' \
		-D CMAKE_BUILD_TYPE='Release' \
		-D CMAKE_INSTALL_PREFIX="${install_directory}" \
		-D OPENSSL_INCLUDE_DIR="${app_directory}/boringssl/include" \
		-D OPENSSL_CRYPTO_LIBRARY="${app_directory}/boringssl/build/${arch}/crypto/libcrypto.a" \
		-D OPENSSL_SSL_LIBRARY="${app_directory}/boringssl/build/${arch}/ssl/libssl.a" \
		-DTD_E2E_ONLY=ON \
		-B "${build_directory}" \
		-S "${source_directory}"

	cmake \
		--build "${build_directory}" \
		--target 'tde2e'

	mkdir --parent "${app_directory}/tde2e/${arch}"

	mv "${build_directory}/tde2e/libtde2e.a" "${app_directory}/tde2e/${arch}/libtde2e.a"
	mv "${build_directory}/tdutils/libtdutils.a" "${app_directory}/tde2e/${arch}/libtdutils.a"
done

echo "Done."
