#import "FuckLogger.h"

@implementation FuckLogger

+ (void)logWarning:(NSString *)message {
    RCTLogWarn(@"%@", message);
}

@end
