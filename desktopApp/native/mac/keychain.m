#import <Foundation/Foundation.h>
#import <Security/Security.h>
#include <jni.h>

// Generic passwords are scoped to this application's service and account. Secrets
// cross JNI as bytes and are never placed in arguments, environment variables or logs.
static NSString *accountString(JNIEnv *env, jstring value) {
    if (!value) return nil;
    const jchar *chars = (*env)->GetStringChars(env, value, NULL);
    if (!chars) return nil;
    NSString *result = [NSString stringWithCharacters:(const unichar *)chars length:(*env)->GetStringLength(env, value)];
    (*env)->ReleaseStringChars(env, value, chars);
    return result;
}
static NSMutableDictionary *queryFor(NSString *account) {
    return [@{(__bridge id)kSecClass: (__bridge id)kSecClassGenericPassword,
              (__bridge id)kSecAttrService: @"com.music.mavimusic.sessions",
              (__bridge id)kSecAttrAccount: account} mutableCopy];
}
static void throwStatus(JNIEnv *env, OSStatus status) {
    // Numeric status only: never include the account or the credential in errors.
    NSString *message = [NSString stringWithFormat:@"Keychain operation failed (%d)", (int)status];
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), message.UTF8String);
}
JNIEXPORT jbyteArray JNICALL Java_com_music_mavimusic_desktop_DesktopMacKeychain_nativeRead(JNIEnv *env, jclass cls, jstring account) {
    @autoreleasepool {
        NSMutableDictionary *query = queryFor(accountString(env, account));
        query[(__bridge id)kSecReturnData] = @YES;
        query[(__bridge id)kSecMatchLimit] = (__bridge id)kSecMatchLimitOne;
        CFTypeRef result = NULL;
        OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)query, &result);
        if (status == errSecItemNotFound) return NULL;
        if (status != errSecSuccess) { throwStatus(env, status); return NULL; }
        NSData *data = CFBridgingRelease(result);
        jbyteArray bytes = (*env)->NewByteArray(env, (jsize)data.length);
        if (bytes) (*env)->SetByteArrayRegion(env, bytes, 0, (jsize)data.length, data.bytes);
        return bytes;
    }
}
JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacKeychain_nativeWrite(JNIEnv *env, jclass cls, jstring account, jbyteArray value) {
    @autoreleasepool {
        jsize length = (*env)->GetArrayLength(env, value);
        NSMutableData *data = [NSMutableData dataWithLength:(NSUInteger)length];
        (*env)->GetByteArrayRegion(env, value, 0, length, data.mutableBytes);
        NSMutableDictionary *query = queryFor(accountString(env, account));
        NSDictionary *update = @{(__bridge id)kSecValueData: data};
        OSStatus status = SecItemUpdate((__bridge CFDictionaryRef)query, (__bridge CFDictionaryRef)update);
        if (status == errSecItemNotFound) {
            query[(__bridge id)kSecValueData] = data;
            query[(__bridge id)kSecAttrAccessible] = (__bridge id)kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly;
            status = SecItemAdd((__bridge CFDictionaryRef)query, NULL);
        }
        [data resetBytesInRange:NSMakeRange(0, data.length)];
        if (status != errSecSuccess) throwStatus(env, status);
    }
}
JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacKeychain_nativeDelete(JNIEnv *env, jclass cls, jstring account) {
    @autoreleasepool {
        OSStatus status = SecItemDelete((__bridge CFDictionaryRef)queryFor(accountString(env, account)));
        if (status != errSecSuccess && status != errSecItemNotFound) throwStatus(env, status);
    }
}
