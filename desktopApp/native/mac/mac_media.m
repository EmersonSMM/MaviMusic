#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>
#import <AppKit/AppKit.h>
#import <Carbon/Carbon.h>
#import <objc/runtime.h>
#include <jni.h>

static BOOL swizzled_canBecomeKeyWindow(id self, SEL _cmd) {
    return YES;
}

static BOOL swizzled_canBecomeMainWindow(id self, SEL _cmd) {
    return YES;
}

static JavaVM *g_jvm = NULL;
static jclass g_mediaClass = NULL;
static jmethodID g_onButtonMethod = NULL;
static jmethodID g_onSeekMethod = NULL;
static BOOL g_isPlaying = NO;

#define BUTTON_PLAY 0
#define BUTTON_PAUSE 1
#define BUTTON_NEXT 2
#define BUTTON_PREVIOUS 3
#define BUTTON_STOP 4
#define BUTTON_SPOTLIGHT 5
#define BUTTON_SPOTLIGHT_EXTERNAL 6
#define BUTTON_SPOTLIGHT_DISMISS 7

static EventHotKeyRef g_spotlightHotKey1 = NULL;
static EventHotKeyRef g_spotlightHotKey2 = NULL;
static EventHandlerRef g_hotKeyHandlerRef = NULL;
static id g_globalClickMonitor = nil;
static id g_resignKeyObserver = nil;

static void dispatchButton(int button);

static OSStatus spotlightHotKeyHandler(EventHandlerCallRef nextHandler, EventRef anEvent, void *userData) {
    EventHotKeyID hotKeyID;
    GetEventParameter(anEvent, kEventParamDirectObject, typeEventHotKeyID, NULL, sizeof(hotKeyID), NULL, &hotKeyID);
    if (hotKeyID.signature == 'MVL1' || hotKeyID.signature == 'MVL2') {
        BOOL appActive = [NSApp isActive];
        if (!appActive) {
            // User triggered Spotlight from outside MaviMusic: keep the main window hidden
            dispatch_async(dispatch_get_main_queue(), ^{
                for (NSWindow *win in [NSApp windows]) {
                    NSString *title = [win title] ?: @"";
                    if (![title isEqualToString:@"MaviMusic Spotlight"] && win.level == NSNormalWindowLevel) {
                        [win orderOut:nil];
                    }
                }
            });
            dispatchButton(BUTTON_SPOTLIGHT_EXTERNAL);
        } else {
            dispatchButton(BUTTON_SPOTLIGHT);
        }
    }
    return noErr;
}

static void dispatchButton(int button) {
    if (!g_jvm || !g_mediaClass || !g_onButtonMethod) return;
    JNIEnv *env = NULL;
    jint res = (*g_jvm)->GetEnv(g_jvm, (void **)&env, JNI_VERSION_1_6);
    BOOL attached = NO;
    if (res == JNI_EDETACHED) {
        if ((*g_jvm)->AttachCurrentThreadAsDaemon(g_jvm, (void **)&env, NULL) == 0) {
            attached = YES;
        }
    }
    if (env) {
        (*env)->CallStaticVoidMethod(env, g_mediaClass, g_onButtonMethod, (jint)button);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
    }
    if (attached) {
        (*g_jvm)->DetachCurrentThread(g_jvm);
    }
}

static void dispatchSeek(int64_t positionMs) {
    if (!g_jvm || !g_mediaClass || !g_onSeekMethod) return;
    JNIEnv *env = NULL;
    jint res = (*g_jvm)->GetEnv(g_jvm, (void **)&env, JNI_VERSION_1_6);
    BOOL attached = NO;
    if (res == JNI_EDETACHED) {
        if ((*g_jvm)->AttachCurrentThreadAsDaemon(g_jvm, (void **)&env, NULL) == 0) {
            attached = YES;
        }
    }
    if (env) {
        (*env)->CallStaticVoidMethod(env, g_mediaClass, g_onSeekMethod, (jlong)positionMs);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
    }
    if (attached) {
        (*g_jvm)->DetachCurrentThread(g_jvm);
    }
}

JNIEXPORT jboolean JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativeStart(JNIEnv *env, jclass clazz) {
    (*env)->GetJavaVM(env, &g_jvm);
    if (!g_mediaClass) {
        jclass localCls = (*env)->FindClass(env, "com/music/mavimusic/desktop/DesktopMacMedia");
        if (localCls) {
            g_mediaClass = (*env)->NewGlobalRef(env, localCls);
            g_onButtonMethod = (*env)->GetStaticMethodID(env, g_mediaClass, "onButton", "(I)V");
            g_onSeekMethod = (*env)->GetStaticMethodID(env, g_mediaClass, "onSeek", "(J)V");
        }
    }

    MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];

    [commandCenter.playCommand setEnabled:YES];
    [commandCenter.playCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        g_isPlaying = YES;
        dispatchButton(BUTTON_PLAY);
        return MPRemoteCommandHandlerStatusSuccess;
    }];

    [commandCenter.pauseCommand setEnabled:YES];
    [commandCenter.pauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        g_isPlaying = NO;
        dispatchButton(BUTTON_PAUSE);
        return MPRemoteCommandHandlerStatusSuccess;
    }];

    [commandCenter.togglePlayPauseCommand setEnabled:YES];
    [commandCenter.togglePlayPauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        if (g_isPlaying) {
            g_isPlaying = NO;
            dispatchButton(BUTTON_PAUSE);
        } else {
            g_isPlaying = YES;
            dispatchButton(BUTTON_PLAY);
        }
        return MPRemoteCommandHandlerStatusSuccess;
    }];

    [commandCenter.nextTrackCommand setEnabled:YES];
    [commandCenter.nextTrackCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        dispatchButton(BUTTON_NEXT);
        return MPRemoteCommandHandlerStatusSuccess;
    }];

    [commandCenter.previousTrackCommand setEnabled:YES];
    [commandCenter.previousTrackCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        dispatchButton(BUTTON_PREVIOUS);
        return MPRemoteCommandHandlerStatusSuccess;
    }];

    [commandCenter.changePlaybackPositionCommand setEnabled:YES];
    [commandCenter.changePlaybackPositionCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        if ([event isKindOfClass:[MPChangePlaybackPositionCommandEvent class]]) {
            MPChangePlaybackPositionCommandEvent *seekEvent = (MPChangePlaybackPositionCommandEvent *)event;
            int64_t ms = (int64_t)(seekEvent.positionTime * 1000.0);
            dispatchSeek(ms);
            return MPRemoteCommandHandlerStatusSuccess;
        }
        return MPRemoteCommandHandlerStatusCommandFailed;
    }];

    // Register global macOS Spotlight Hotkeys (Cmd + Shift + K and Cmd + K)
    if (!g_hotKeyHandlerRef) {
        EventTypeSpec eventType;
        eventType.eventClass = kEventClassKeyboard;
        eventType.eventKind = kEventHotKeyPressed;
        InstallApplicationEventHandler(&spotlightHotKeyHandler, 1, &eventType, NULL, &g_hotKeyHandlerRef);
    }

    if (!g_spotlightHotKey1) {
        EventHotKeyID hotKeyID1;
        hotKeyID1.signature = 'MVL1';
        hotKeyID1.id = 1;
        // Cmd + Shift + K (kVK_ANSI_K is 40)
        RegisterEventHotKey(40, cmdKey | shiftKey, hotKeyID1, GetApplicationEventTarget(), 0, &g_spotlightHotKey1);
    }

    if (!g_spotlightHotKey2) {
        EventHotKeyID hotKeyID2;
        hotKeyID2.signature = 'MVL2';
        hotKeyID2.id = 2;
        // Cmd + K (kVK_ANSI_K is 40)
        RegisterEventHotKey(40, cmdKey, hotKeyID2, GetApplicationEventTarget(), 0, &g_spotlightHotKey2);
    }

    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativeStop(JNIEnv *env, jclass clazz) {
    if (g_spotlightHotKey1) {
        UnregisterEventHotKey(g_spotlightHotKey1);
        g_spotlightHotKey1 = NULL;
    }
    if (g_spotlightHotKey2) {
        UnregisterEventHotKey(g_spotlightHotKey2);
        g_spotlightHotKey2 = NULL;
    }
    if (g_hotKeyHandlerRef) {
        RemoveEventHandler(g_hotKeyHandlerRef);
        g_hotKeyHandlerRef = NULL;
    }

    MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [commandCenter.playCommand removeTarget:nil];
    [commandCenter.pauseCommand removeTarget:nil];
    [commandCenter.togglePlayPauseCommand removeTarget:nil];
    [commandCenter.nextTrackCommand removeTarget:nil];
    [commandCenter.previousTrackCommand removeTarget:nil];
    [commandCenter.changePlaybackPositionCommand removeTarget:nil];

    MPNowPlayingInfoCenter *infoCenter = [MPNowPlayingInfoCenter defaultCenter];
    infoCenter.nowPlayingInfo = nil;
    if (@available(macOS 10.13, *)) {
        infoCenter.playbackState = MPNowPlayingPlaybackStateStopped;
    }
}

static MPMediaItemArtwork *createArtwork(NSImage *originalImage) {
    if (!originalImage) return nil;
    CGSize imageSize = originalImage.size;
    if (imageSize.width <= 0 || imageSize.height <= 0) {
        for (NSImageRep *rep in [originalImage representations]) {
            if (rep.pixelsWide > 0 && rep.pixelsHigh > 0) {
                imageSize = CGSizeMake(rep.pixelsWide, rep.pixelsHigh);
                [originalImage setSize:imageSize];
                break;
            }
        }
    }
    if (imageSize.width <= 0 || imageSize.height <= 0) {
        imageSize = CGSizeMake(512, 512);
        [originalImage setSize:imageSize];
    }

    return [[MPMediaItemArtwork alloc] initWithBoundsSize:imageSize requestHandler:^NSImage * _Nonnull(CGSize requestedSize) {
        if (requestedSize.width <= 0 || requestedSize.height <= 0) {
            return originalImage;
        }
        if (fabs(requestedSize.width - imageSize.width) < 1.0 && fabs(requestedSize.height - imageSize.height) < 1.0) {
            return originalImage;
        }
        return [NSImage imageWithSize:requestedSize flipped:NO drawingHandler:^BOOL(NSRect dstRect) {
            [originalImage drawInRect:dstRect
                             fromRect:NSMakeRect(0, 0, imageSize.width, imageSize.height)
                            operation:NSCompositingOperationSourceOver
                             fraction:1.0];
            return YES;
        }];
    }];
}

static NSImage *loadImageFromPathOrUrl(NSString *pathOrUrl) {
    if (!pathOrUrl || pathOrUrl.length == 0) return nil;
    if ([pathOrUrl hasPrefix:@"http://"] || [pathOrUrl hasPrefix:@"https://"]) {
        NSURL *url = [NSURL URLWithString:pathOrUrl];
        if (url) {
            NSData *data = [NSData dataWithContentsOfURL:url];
            if (data) {
                return [[NSImage alloc] initWithData:data];
            }
        }
        return nil;
    }
    NSImage *img = [[NSImage alloc] initWithContentsOfFile:pathOrUrl];
    if (!img) {
        NSData *data = [NSData dataWithContentsOfFile:pathOrUrl];
        if (data) {
            img = [[NSImage alloc] initWithData:data];
        }
    }
    return img;
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativeUpdate(
    JNIEnv *env,
    jclass clazz,
    jstring title,
    jstring artist,
    jstring album,
    jstring artworkPath,
    jboolean isPlaying,
    jlong positionMs,
    jlong durationMs
) {
    @autoreleasepool {
        MPNowPlayingInfoCenter *infoCenter = [MPNowPlayingInfoCenter defaultCenter];
        g_isPlaying = (isPlaying == JNI_TRUE);

        NSString *sTitle = nil;
        NSString *sArtist = nil;
        NSString *sAlbum = nil;
        NSString *sArtwork = nil;

        if (title) {
            const char *titleUtf8 = (*env)->GetStringUTFChars(env, title, NULL);
            if (titleUtf8) {
                sTitle = [NSString stringWithUTF8String:titleUtf8];
                (*env)->ReleaseStringUTFChars(env, title, titleUtf8);
            }
        }
        if (artist) {
            const char *artistUtf8 = (*env)->GetStringUTFChars(env, artist, NULL);
            if (artistUtf8) {
                sArtist = [NSString stringWithUTF8String:artistUtf8];
                (*env)->ReleaseStringUTFChars(env, artist, artistUtf8);
            }
        }
        if (album) {
            const char *albumUtf8 = (*env)->GetStringUTFChars(env, album, NULL);
            if (albumUtf8) {
                sAlbum = [NSString stringWithUTF8String:albumUtf8];
                (*env)->ReleaseStringUTFChars(env, album, albumUtf8);
            }
        }
        if (artworkPath) {
            const char *artUtf8 = (*env)->GetStringUTFChars(env, artworkPath, NULL);
            if (artUtf8) {
                sArtwork = [NSString stringWithUTF8String:artUtf8];
                (*env)->ReleaseStringUTFChars(env, artworkPath, artUtf8);
            }
        }

        if ((!sTitle || sTitle.length == 0) && durationMs <= 0) {
            infoCenter.nowPlayingInfo = nil;
            if (@available(macOS 10.13, *)) {
                infoCenter.playbackState = MPNowPlayingPlaybackStateStopped;
            }
            return;
        }

        NSMutableDictionary *info = [NSMutableDictionary dictionary];
        if (sTitle && sTitle.length > 0) info[MPMediaItemPropertyTitle] = sTitle;
        if (sArtist && sArtist.length > 0) info[MPMediaItemPropertyArtist] = sArtist;
        if (sAlbum && sAlbum.length > 0) info[MPMediaItemPropertyAlbumTitle] = sAlbum;

        double durationSec = (double)durationMs / 1000.0;
        double positionSec = (double)positionMs / 1000.0;

        info[MPMediaItemPropertyPlaybackDuration] = @(durationSec);
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(positionSec);
        info[MPNowPlayingInfoPropertyPlaybackRate] = @(isPlaying ? 1.0 : 0.0);

        BOOL artworkAssigned = NO;
        if (sArtwork && sArtwork.length > 0) {
            if (![sArtwork hasPrefix:@"http://"] && ![sArtwork hasPrefix:@"https://"]) {
                NSImage *localImage = loadImageFromPathOrUrl(sArtwork);
                if (localImage) {
                    MPMediaItemArtwork *artwork = createArtwork(localImage);
                    if (artwork) {
                        info[MPMediaItemPropertyArtwork] = artwork;
                        artworkAssigned = YES;
                    }
                }
            }
        }

        if (!artworkAssigned && infoCenter.nowPlayingInfo && infoCenter.nowPlayingInfo[MPMediaItemPropertyArtwork]) {
            NSString *existingTitle = infoCenter.nowPlayingInfo[MPMediaItemPropertyTitle];
            if (sTitle && [existingTitle isEqualToString:sTitle]) {
                info[MPMediaItemPropertyArtwork] = infoCenter.nowPlayingInfo[MPMediaItemPropertyArtwork];
                artworkAssigned = YES;
            }
        }

        infoCenter.nowPlayingInfo = info;
        if (@available(macOS 10.13, *)) {
            infoCenter.playbackState = isPlaying ? MPNowPlayingPlaybackStatePlaying : MPNowPlayingPlaybackStatePaused;
        }

        if (!artworkAssigned && sArtwork && ([sArtwork hasPrefix:@"http://"] || [sArtwork hasPrefix:@"https://"])) {
            NSString *urlStr = [sArtwork copy];
            NSString *trackTitle = [sTitle copy];
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                NSImage *fetchedImage = loadImageFromPathOrUrl(urlStr);
                if (fetchedImage) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
                        NSMutableDictionary *curr = [center.nowPlayingInfo mutableCopy];
                        if (curr && trackTitle && [curr[MPMediaItemPropertyTitle] isEqualToString:trackTitle]) {
                            MPMediaItemArtwork *art = createArtwork(fetchedImage);
                            if (art) {
                                curr[MPMediaItemPropertyArtwork] = art;
                                center.nowPlayingInfo = curr;
                            }
                        }
                    });
                }
            });
        }
    }
}

@interface MaviNotificationDelegate : NSObject <NSUserNotificationCenterDelegate>
@end

@implementation MaviNotificationDelegate
- (BOOL)userNotificationCenter:(NSUserNotificationCenter *)center shouldPresentNotification:(NSUserNotification *)notification {
    return YES;
}
@end

static MaviNotificationDelegate *g_notifDelegate = nil;

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativePostNotification(
    JNIEnv *env,
    jclass clazz,
    jstring title,
    jstring subtitle,
    jstring body,
    jstring imagePath
) {
    @autoreleasepool {
        if (!g_notifDelegate) {
            g_notifDelegate = [[MaviNotificationDelegate alloc] init];
            [[NSUserNotificationCenter defaultUserNotificationCenter] setDelegate:g_notifDelegate];
        }

        NSString *sTitle = nil;
        NSString *sSub = nil;
        NSString *sBody = nil;
        NSString *sPath = nil;

        if (title) {
            const char *cTitle = (*env)->GetStringUTFChars(env, title, NULL);
            if (cTitle) {
                sTitle = [NSString stringWithUTF8String:cTitle];
                (*env)->ReleaseStringUTFChars(env, title, cTitle);
            }
        }
        if (subtitle) {
            const char *cSub = (*env)->GetStringUTFChars(env, subtitle, NULL);
            if (cSub) {
                sSub = [NSString stringWithUTF8String:cSub];
                (*env)->ReleaseStringUTFChars(env, subtitle, cSub);
            }
        }
        if (body) {
            const char *cBody = (*env)->GetStringUTFChars(env, body, NULL);
            if (cBody) {
                sBody = [NSString stringWithUTF8String:cBody];
                (*env)->ReleaseStringUTFChars(env, body, cBody);
            }
        }
        if (imagePath) {
            const char *cImg = (*env)->GetStringUTFChars(env, imagePath, NULL);
            if (cImg) {
                sPath = [NSString stringWithUTF8String:cImg];
                (*env)->ReleaseStringUTFChars(env, imagePath, cImg);
            }
        }

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSImage *image = loadImageFromPathOrUrl(sPath);
            dispatch_async(dispatch_get_main_queue(), ^{
                NSUserNotification *notification = [[NSUserNotification alloc] init];
                if (sTitle) notification.title = sTitle;
                if (sSub) notification.subtitle = sSub;
                if (sBody) notification.informativeText = sBody;
                if (image) notification.contentImage = image;
                [[NSUserNotificationCenter defaultUserNotificationCenter] deliverNotification:notification];
            });
        });
    }
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativeConfigureSpotlight(
    JNIEnv *env,
    jclass clazz,
    jboolean keepMainHidden
) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @try {
            NSWindow *spotWin = nil;
            for (NSWindow *win in [NSApp windows]) {
                NSString *title = [win title] ?: @"";
                if ([title isEqualToString:@"MaviMusic Spotlight"]) {
                    spotWin = win;
                } else if (keepMainHidden && win.level == NSNormalWindowLevel) {
                    [win orderOut:nil];
                }
            }
            if (spotWin) {
                Class winClass = [spotWin class];
                static dispatch_once_t onceToken;
                dispatch_once(&onceToken, ^{
                    Method mKey = class_getInstanceMethod(winClass, @selector(canBecomeKeyWindow));
                    if (mKey) {
                        method_setImplementation(mKey, (IMP)swizzled_canBecomeKeyWindow);
                    } else {
                        class_addMethod(winClass, @selector(canBecomeKeyWindow), (IMP)swizzled_canBecomeKeyWindow, "c@:");
                    }

                    Method mMain = class_getInstanceMethod(winClass, @selector(canBecomeMainWindow));
                    if (mMain) {
                        method_setImplementation(mMain, (IMP)swizzled_canBecomeMainWindow);
                    } else {
                        class_addMethod(winClass, @selector(canBecomeMainWindow), (IMP)swizzled_canBecomeMainWindow, "c@:");
                    }
                });

                [spotWin setLevel:NSFloatingWindowLevel];
                [spotWin setCollectionBehavior:(NSWindowCollectionBehaviorCanJoinAllSpaces |
                                                NSWindowCollectionBehaviorFullScreenAuxiliary)];
                [spotWin setMovableByWindowBackground:YES];
                [spotWin setHidesOnDeactivate:NO];

                [NSApp activateIgnoringOtherApps:YES];
                [spotWin makeKeyAndOrderFront:nil];
                [spotWin makeKeyWindow];

                if (g_globalClickMonitor) {
                    [NSEvent removeMonitor:g_globalClickMonitor];
                    g_globalClickMonitor = nil;
                }
                g_globalClickMonitor = [NSEvent addGlobalMonitorForEventsMatchingMask:(NSEventMaskLeftMouseDown | NSEventMaskRightMouseDown)
                                                                             handler:^(NSEvent *event) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        dispatchButton(BUTTON_SPOTLIGHT_DISMISS);
                    });
                }];
            }
        } @catch (NSException *e) {
            NSLog(@"[MaviMusic] Error configuring spotlight window: %@", e);
        }
    });
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativeOnSpotlightDismissed(
    JNIEnv *env,
    jclass clazz,
    jboolean hideApp
) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (g_globalClickMonitor) {
            [NSEvent removeMonitor:g_globalClickMonitor];
            g_globalClickMonitor = nil;
        }
        if (g_resignKeyObserver) {
            [[NSNotificationCenter defaultCenter] removeObserver:g_resignKeyObserver];
            g_resignKeyObserver = nil;
        }
        if (hideApp) {
            [NSApp hide:nil];
        }
    });
}

JNIEXPORT void JNICALL Java_com_music_mavimusic_desktop_DesktopMacMedia_nativeEnforceMainMinSize(
    JNIEnv *env,
    jclass clazz,
    jint minW,
    jint minH
) {
    dispatch_async(dispatch_get_main_queue(), ^{
        for (NSWindow *win in [NSApp windows]) {
            NSString *title = [win title] ?: @"";
            if (![title isEqualToString:@"MaviMusic Spotlight"] && (win.styleMask & NSWindowStyleMaskResizable)) {
                [win setMinSize:NSMakeSize((CGFloat)minW, (CGFloat)minH)];
                [win setContentMinSize:NSMakeSize((CGFloat)minW, (CGFloat)minH)];
                NSRect frame = [win frame];
                BOOL changed = NO;
                if (frame.size.width < minW) {
                    frame.size.width = minW;
                    changed = YES;
                }
                if (frame.size.height < minH) {
                    frame.size.height = minH;
                    changed = YES;
                }
                if (changed) {
                    [win setFrame:frame display:YES animate:NO];
                }
            }
        }
    });
}


