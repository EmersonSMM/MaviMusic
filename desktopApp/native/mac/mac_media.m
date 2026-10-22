#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>
#import <AppKit/AppKit.h>
#include <jni.h>

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

JNIEXPORT jboolean JNICALL Java_com_music_bitchord_desktop_DesktopMacMedia_nativeStart(JNIEnv *env, jclass clazz) {
    (*env)->GetJavaVM(env, &g_jvm);
    if (!g_mediaClass) {
        jclass localCls = (*env)->FindClass(env, "com/music/bitchord/desktop/DesktopMacMedia");
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

    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_music_bitchord_desktop_DesktopMacMedia_nativeStop(JNIEnv *env, jclass clazz) {
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

JNIEXPORT void JNICALL Java_com_music_bitchord_desktop_DesktopMacMedia_nativeUpdate(
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

        if (!title && durationMs <= 0) {
            infoCenter.nowPlayingInfo = nil;
            if (@available(macOS 10.13, *)) {
                infoCenter.playbackState = MPNowPlayingPlaybackStateStopped;
            }
            return;
        }

        NSMutableDictionary *info = [NSMutableDictionary dictionary];

        if (title) {
            const char *titleUtf8 = (*env)->GetStringUTFChars(env, title, NULL);
            if (titleUtf8) {
                info[MPMediaItemPropertyTitle] = [NSString stringWithUTF8String:titleUtf8];
                (*env)->ReleaseStringUTFChars(env, title, titleUtf8);
            }
        }
        if (artist) {
            const char *artistUtf8 = (*env)->GetStringUTFChars(env, artist, NULL);
            if (artistUtf8) {
                info[MPMediaItemPropertyArtist] = [NSString stringWithUTF8String:artistUtf8];
                (*env)->ReleaseStringUTFChars(env, artist, artistUtf8);
            }
        }
        if (album) {
            const char *albumUtf8 = (*env)->GetStringUTFChars(env, album, NULL);
            if (albumUtf8) {
                info[MPMediaItemPropertyAlbumTitle] = [NSString stringWithUTF8String:albumUtf8];
                (*env)->ReleaseStringUTFChars(env, album, albumUtf8);
            }
        }

        if (artworkPath) {
            const char *artUtf8 = (*env)->GetStringUTFChars(env, artworkPath, NULL);
            if (artUtf8) {
                if (strlen(artUtf8) > 0) {
                    NSString *path = [NSString stringWithUTF8String:artUtf8];
                    NSImage *image = [[NSImage alloc] initWithContentsOfFile:path];
                    if (image) {
                        MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithBoundsSize:image.size requestHandler:^NSImage * _Nonnull(CGSize size) {
                            return image;
                        }];
                        info[MPMediaItemPropertyArtwork] = artwork;
                    }
                }
                (*env)->ReleaseStringUTFChars(env, artworkPath, artUtf8);
            }
        }

        double durationSec = (double)durationMs / 1000.0;
        double positionSec = (double)positionMs / 1000.0;

        info[MPMediaItemPropertyPlaybackDuration] = @(durationSec);
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(positionSec);
        info[MPNowPlayingInfoPropertyPlaybackRate] = @(isPlaying ? 1.0 : 0.0);

        infoCenter.nowPlayingInfo = info;
        if (@available(macOS 10.13, *)) {
            infoCenter.playbackState = isPlaying ? MPNowPlayingPlaybackStatePlaying : MPNowPlayingPlaybackStatePaused;
        }
    }
}
