// Wallet engine stub: compiled into tmessages when WITH_WALLET_ENGINE=OFF.
// Provides the same JNI entry points as wallet/WalletEngineBridge.cpp so the
// Java side links and runs, but every operation reports NO_WALLET_ENGINE.
// This keeps libwallet_engine.a (~24-29 MB per ABI) out of the final .so.
//
// Predicates return safe defaults (false); void lifecycle calls are no-ops;
// everything else throws IllegalStateException("NO_WALLET_ENGINE"), which the
// Java callers already handle (see WalletEngine2 error paths).
#include <jni.h>

namespace {
void throw_no_engine(JNIEnv *env) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls) {
        env->ThrowNew(cls, "NO_WALLET_ENGINE");
        env->DeleteLocalRef(cls);
    }
}
} // namespace

extern "C" JNIEXPORT jobjectArray JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_mnemonicWordlist(JNIEnv *env, jclass) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeSecretPhraseToPublicKey(
        JNIEnv *env, jclass, jbyteArray) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeSecretPhraseToAddress(
        JNIEnv *env, jclass, jbyteArray) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_isValidCellBoc(JNIEnv *, jclass, jstring) {
    return JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeTonConnectCrypto(
        JNIEnv *env, jclass, jbyteArray, jbyteArray) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeCreateImportProof(
        JNIEnv *env, jclass, jbyteArray, jbyteArray, jbyteArray, jint, jboolean) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeIsValidAddress(
        JNIEnv *, jclass, jstring) {
    return JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeTransactionAddress(
        JNIEnv *env, jclass, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeCreate(
        JNIEnv *env, jclass, jint, jstring, jbyteArray, jobject, jstring) {
    throw_no_engine(env);
    return 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeBalance(JNIEnv *env, jclass, jlong) {
    throw_no_engine(env);
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeEmulateSend(
        JNIEnv *env, jclass, jlong, jstring, jlong, jbyteArray, jboolean) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeEmulateRotateKey(
        JNIEnv *env, jclass, jlong, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativePrepareRotateKey(
        JNIEnv *env, jclass, jlong, jbyteArray, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeDecryptRawComment(
        JNIEnv *env, jclass, jbyteArray, jstring, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeDecryptComment(
        JNIEnv *env, jclass, jlong, jbyteArray, jstring, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativePreviewTonConnect(
        JNIEnv *env, jclass, jlong, jobjectArray, jlong) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativePreviewSignMessage(
        JNIEnv *env, jclass, jlong, jobjectArray, jlong) {
    throw_no_engine(env);
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeSignMessage(
        JNIEnv *env, jclass, jlong, jbyteArray, jobjectArray, jlong, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativePrepareTonConnectTransfer(
        JNIEnv *env, jclass, jlong, jbyteArray, jobjectArray, jlong, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativePrepareSend(
        JNIEnv *env, jclass, jlong, jbyteArray, jstring, jlong, jbyteArray,
        jbyteArray, jstring, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeMessageBodyHash(
        JNIEnv *env, jclass, jstring) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeSend(
        JNIEnv *env, jclass, jlong, jbyteArray, jstring, jlong, jbyteArray,
        jbyteArray, jstring, jobject) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeResolvePending(JNIEnv *env, jclass, jlong) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeRotateKey(
        JNIEnv *env, jclass, jlong, jbyteArray, jstring, jobject) {
    throw_no_engine(env);
}

extern "C" JNIEXPORT void JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeClose(JNIEnv *, jclass, jlong) {
    // No-op: there is no engine instance to shut down.
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativeEmulateSendNFT(
        JNIEnv *env, jclass, jlong, jstring, jstring, jbyteArray, jstring, jlong, jlong) {
    throw_no_engine(env);
    return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_telegram_ui_Wallet_WalletEngine2_nativePrepareSendNFT(
        JNIEnv *env, jclass, jlong, jbyteArray, jstring, jstring, jbyteArray,
        jstring, jlong, jlong) {
    throw_no_engine(env);
    return nullptr;
}
