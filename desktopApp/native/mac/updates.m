#import <AppKit/AppKit.h>
#import <Sparkle/Sparkle.h>
#include <jni.h>

static JavaVM *updateVM;
static jclass updateClass;
static jmethodID eventMethod;
static SPUStandardUpdaterController *updateController;
static id updateDelegate;
static void (^pendingInstall)(void);

static void emitUpdateEvent(int event, NSString *message) {
    if (!updateVM || !updateClass) return;
    JNIEnv *env = NULL;
    BOOL attached = NO;
    if ((*updateVM)->GetEnv(updateVM, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*updateVM)->AttachCurrentThreadAsDaemon(updateVM, (void **)&env, NULL) != JNI_OK) return;
        attached = YES;
    }
    NSString *text = message ?: @"";
    NSUInteger length = text.length;
    unichar *characters = malloc(MAX(length, 1) * sizeof(unichar));
    if (!characters) return;
    [text getCharacters:characters range:NSMakeRange(0, length)];
    jstring value = (*env)->NewString(env, (const jchar *)characters, (jsize)length);
    free(characters);
    (*env)->CallStaticVoidMethod(env, updateClass, eventMethod, event, value);
    (*env)->DeleteLocalRef(env, value);
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
    if (attached) (*updateVM)->DetachCurrentThread(updateVM);
}

@interface MaviUpdateDelegate : NSObject <SPUUpdaterDelegate>
@end
@implementation MaviUpdateDelegate
- (void)updater:(SPUUpdater *)updater didFindValidUpdate:(SUAppcastItem *)item {
    emitUpdateEvent(1, item.displayVersionString);
}
- (void)updater:(SPUUpdater *)updater didFinishUpdateCycleForUpdateCheck:(SPUUpdateCheck)check error:(NSError *)error {
    emitUpdateEvent(2, error && error.code != SUNoUpdateError ? error.localizedDescription : @"");
}
- (BOOL)updater:(SPUUpdater *)updater shouldPostponeRelaunchForUpdate:(SUAppcastItem *)item untilInvokingBlock:(void (^)(void))handler {
    pendingInstall = [handler copy];
    emitUpdateEvent(3, @"");
    return YES;
}
@end

JNIEXPORT jstring JNICALL Java_com_music_mavimusic_desktop_DesktopMacUpdater_nativeStart(JNIEnv *env, jobject self) {
    if (!updateVM) {
        (*env)->GetJavaVM(env, &updateVM);
        jclass cls = (*env)->GetObjectClass(env, self);
        updateClass = (*env)->NewGlobalRef(env, cls);
        eventMethod = (*env)->GetStaticMethodID(env, cls, "onNativeEvent", "(ILjava/lang/String;)V");
        (*env)->DeleteLocalRef(env, cls);
    }
    __block NSString *failure = nil;
    void (^start)(void) = ^{
        if (updateController) return;
        NSBundle *bundle = NSBundle.mainBundle;
        if (![bundle.bundleIdentifier isEqualToString:@"com.music.mavimusic"] || ![bundle objectForInfoDictionaryKey:@"SUPublicEDKey"]) {
            failure = @"Las actualizaciones requieren la aplicación instalada.";
            return;
        }
        updateDelegate = [MaviUpdateDelegate new];
        updateController = [[SPUStandardUpdaterController alloc] initWithStartingUpdater:NO updaterDelegate:updateDelegate userDriverDelegate:nil];
        NSError *error = nil;
        if (![updateController.updater startUpdater:&error]) {
            failure = error.localizedDescription;
            updateController = nil;
        }
    };
    if (NSThread.isMainThread) start(); else dispatch_sync(dispatch_get_main_queue(), start);
    return (*env)->NewStringUTF(env, (failure ?: @"").UTF8String);
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacUpdater_nativeCheck(JNIEnv *env, jobject self) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (updateController.updater.canCheckForUpdates) [updateController checkForUpdates:nil];
    });
}

JNIEXPORT jint JNICALL Java_com_music_mavimusic_desktop_DesktopMacUpdater_nativePreferences(JNIEnv *env, jobject self) {
    __block int state = 0;
    void (^read)(void) = ^{
        if (updateController.updater.automaticallyChecksForUpdates) state |= 1;
        if (updateController.updater.automaticallyDownloadsUpdates) state |= 2;
    };
    if (NSThread.isMainThread) read(); else dispatch_sync(dispatch_get_main_queue(), read);
    return state;
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacUpdater_nativeConfigure(JNIEnv *env, jobject self, jboolean checks, jboolean downloads) {
    dispatch_async(dispatch_get_main_queue(), ^{
        updateController.updater.automaticallyChecksForUpdates = checks;
        updateController.updater.automaticallyDownloadsUpdates = downloads;
    });
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacUpdater_nativeReadyToInstall(JNIEnv *env, jobject self) {
    dispatch_async(dispatch_get_main_queue(), ^{
        void (^handler)(void) = pendingInstall;
        pendingInstall = nil;
        if (handler) handler();
    });
}
