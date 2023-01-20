//
//  RNSoundPlayer
//
//  Created by Johnson Su on 2018-07-10.
//

#import <React/RCTBridgeModule.h>
#import <AVFoundation/AVFoundation.h>
#import <React/RCTEventEmitter.h>
#import "MuteChecker.h"

@interface RNSoundPlayer : RCTEventEmitter <RCTBridgeModule, AVAudioPlayerDelegate>
@property (nonatomic, strong) AVAudioPlayer *player;
@property (nonatomic) int loopCount;
@property (nonatomic, strong) MuteChecker *muteChecker;
@end
