//
//  RNSoundPlayer
//
//  Created by Johnson Su on 2018-07-10.
//

#import "RNSoundPlayer.h"

@implementation RNSoundPlayer

static NSString *const EVENT_FINISHED_LOADING = @"FinishedLoading";
static NSString *const EVENT_FINISHED_LOADING_FILE = @"FinishedLoadingFile";
static NSString *const EVENT_FINISHED_LOADING_URL = @"FinishedLoadingURL";
static NSString *const EVENT_FINISHED_PLAYING = @"FinishedPlaying";


RCT_EXPORT_METHOD(playUrl:(NSString *)url) {
    [self prepareUrl:url];
    [self.player play];
}

RCT_EXPORT_METHOD(loadUrl:(NSString *)url) {
    [self prepareUrl:url];
}

RCT_EXPORT_METHOD(playSoundFile:(NSString *)name ofType:(NSString *)type) {
    [self mountSoundFile:name ofType:type];
    [self.player play];
}

RCT_EXPORT_METHOD(playSoundFileWithDelay:(NSString *)name ofType:(NSString *)type delay:(double)delay) {
    [self mountSoundFile:name ofType:type];
    [self.player playAtTime:(self.player.deviceCurrentTime + delay)];
}

RCT_EXPORT_METHOD(loadSoundFile:(NSString *)name ofType:(NSString *)type) {
    [self mountSoundFile:name ofType:type];
}

- (NSArray<NSString *> *)supportedEvents {
    return @[EVENT_FINISHED_PLAYING, EVENT_FINISHED_LOADING, EVENT_FINISHED_LOADING_URL, EVENT_FINISHED_LOADING_FILE];
}

RCT_EXPORT_METHOD(pause) {
    if (self.player != nil) {
        [self.player pause];
    }
}

RCT_EXPORT_METHOD(resume) {
    if (self.player != nil) {
        [self.player play];
    }
}

RCT_EXPORT_METHOD(stop) {
    if (self.player != nil) {
        [self.player stop];
    }
}

RCT_EXPORT_METHOD(seek:(float)seconds) {
    if (self.player != nil) {
        self.player.currentTime = seconds;
    }
}

RCT_EXPORT_METHOD(setSpeaker:(BOOL) on) {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    if (on) {
        [session setCategory: AVAudioSessionCategoryPlayAndRecord error: nil];
        [session overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:nil];
    } else {
        [session setCategory: AVAudioSessionCategoryPlayback error: nil];
        [session overrideOutputAudioPort:AVAudioSessionPortOverrideNone error:nil];
    }
    [session setActive:true error:nil];
}

RCT_EXPORT_METHOD(setVolume:(float)volume) {
    if (self.player != nil) {
        [self.player setVolume: volume];
    }
}

RCT_EXPORT_METHOD(getDeviceVolume:(RCTPromiseResolveBlock) resolve
    rejecter:(RCTPromiseRejectBlock) reject) {
    float state = [AVAudioSession.sharedInstance outputVolume];
    resolve(@(state));
}

RCT_EXPORT_METHOD(isDeviceMuted:(RCTPromiseResolveBlock) resolve
                  rejecter:(RCTPromiseRejectBlock) reject) {
    self.muteChecker = [[MuteChecker alloc] initWithCompletionBlk:^(BOOL muted) {
            resolve(muted ? @TRUE : @FALSE);
        }];
    @try {
        [_muteChecker check];
    }
    @catch (NSException *e) {
        reject(@"E_IS_DEVICE_MUTED", @"Error occured when checking is muted.", [NSError errorWithDomain:e.name code:0 userInfo:@{
        NSUnderlyingErrorKey: e,
        NSDebugDescriptionErrorKey: e.userInfo ?: @{ },
        NSLocalizedFailureReasonErrorKey: (e.reason ?: @"???") }]);
    }
}

RCT_REMAP_METHOD(getInfo,
                 getInfoWithResolver:(RCTPromiseResolveBlock) resolve
                 rejecter:(RCTPromiseRejectBlock) reject) {
    if (self.player != nil) {
        NSDictionary *data = @{
                               @"currentTime": [NSNumber numberWithDouble:[self.player currentTime]],
                               @"duration": [NSNumber numberWithDouble:[self.player duration]]
                               };
        resolve(data);
    }
    resolve(nil);
}

- (void) audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    [self sendEventWithName:EVENT_FINISHED_PLAYING body:@{@"success": [NSNumber numberWithBool:flag]}];
}

- (void) audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error {
    NSLog(@"%@",[error localizedDescription]);
    [self sendEventWithName:EVENT_FINISHED_PLAYING body:@{@"success": [NSNumber numberWithBool:false]}];
}

- (void) itemDidFinishPlaying:(NSNotification *) notification {
    [self sendEventWithName:EVENT_FINISHED_PLAYING body:@{@"success": [NSNumber numberWithBool:TRUE]}];
}

- (void) mountSoundFile:(NSString *)name ofType:(NSString *)type {
    NSString *soundFilePath = [[NSBundle mainBundle] pathForResource:name ofType:type];

    if (soundFilePath == nil) {
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];
        soundFilePath = [NSString stringWithFormat:@"%@.%@", [documentsDirectory stringByAppendingPathComponent:[NSString stringWithFormat:@"%@",name]], type];
    }

    NSURL *soundFileURL = [NSURL fileURLWithPath:soundFilePath];
    self.player = [[AVAudioPlayer alloc] initWithContentsOfURL:soundFileURL error:nil];
    [self.player setDelegate:self];
    [self.player setNumberOfLoops:self.loopCount];
    [self.player prepareToPlay];
    [[AVAudioSession sharedInstance]
            setCategory: AVAudioSessionCategoryPlayback
            error: nil];
    [self sendEventWithName:EVENT_FINISHED_LOADING body:@{@"success": [NSNumber numberWithBool:true]}];
    [self sendEventWithName:EVENT_FINISHED_LOADING_FILE body:@{@"success": [NSNumber numberWithBool:true], @"name": name, @"type": type}];
}

- (void) prepareUrl:(NSString *)url {
    NSURL *soundURL = [NSURL URLWithString:url];
    NSError *error;
    NSData *data = [NSData dataWithContentsOfURL:soundURL];
    self.player = [[AVAudioPlayer alloc] initWithData:data error:&error];
    if (self.player) {
        [self.player setDelegate:self];
        self.player.enableRate = YES;
        [self.player prepareToPlay];
        [self sendEventWithName:EVENT_FINISHED_LOADING body:@{@"success": [NSNumber numberWithBool:true]}];
        [self sendEventWithName:EVENT_FINISHED_LOADING_URL body: @{@"success": [NSNumber numberWithBool:true], @"url": url}];
    } else {
        NSLog(@"Description: %@",[error description]);
        NSLog(@"LocalizedDescription: %@",[error localizedDescription]);
        [self sendEventWithName:EVENT_FINISHED_LOADING body:@{@"success": [NSNumber numberWithBool:false], @"iosErrorCode": [NSNumber numberWithInteger:error.code], @"iosErrorDescription": [error localizedDescription]}];
        [self sendEventWithName:EVENT_FINISHED_LOADING_URL body: @{@"success": [NSNumber numberWithBool:false], @"url": url, @"iosErrorCode": [NSNumber numberWithInteger:error.code], @"iosErrorDescription": [error localizedDescription]}];
    }
}

RCT_EXPORT_MODULE();

@end
