package application;

import java.util.concurrent.ScheduledExecutorService;

import org.opencv.core.Point;

public class FingerTouchObserver {

	ScheduledExecutorService scheduledTask;
	static int oldLength = 0;
	static int newLength = 0;
	static int oldAngle = 0;
	static int newAngle = 0;
	
	static int oldAction = 0;
	static int newAction = 0;

	public static int didMakeTouch(Point midPoint, Point startIndex, int angle){
		int didTouch = EnvironmentVariables.didNotMakeTouch;
		int lengthCheck = checkForLengthChange(midPoint, startIndex);
		int angleCheck = checkForAngleChange(angle);
		
		if (lengthCheck == EnvironmentVariables.lengthPressDown
				&& angleCheck == EnvironmentVariables.anglePressDown) {
			newAction = EnvironmentVariables.touchDown;
			
		}else if (lengthCheck == EnvironmentVariables.lengthPressUp 
				&& angleCheck == EnvironmentVariables.anglePressup) {
			newAction = EnvironmentVariables.touchUp;
			if (newAction != oldAction) {
				didTouch = EnvironmentVariables.didMakeTouch;
			}else{
				didTouch = EnvironmentVariables.didNotMakeTouch;
			}
				
		}
		oldAction = newAction;
		return didTouch;
	}
	
	
	public static int checkForLengthChange(Point midPoint, Point startIndex){
		
		int touchStatus = 0;
		newLength = calculateLineLength(midPoint, startIndex);
		
		if (newLength != oldLength) {
			if (newLength > oldLength +10) {
				touchStatus = EnvironmentVariables.lengthPressDown;
			}else if (newLength < oldLength -10){
				touchStatus = EnvironmentVariables.lengthPressUp;
			}else{
				touchStatus = EnvironmentVariables.noTouchAction;
			}
			oldLength = newLength;
			newLength = 0;
		}
		return touchStatus;
	}
	
	public static int checkForAngleChange(int angle){
		
		int touchStatus = 0;
		newAngle = angle;
		
		if (newAngle != oldAngle) {
			if (newAngle > oldAngle +10) {
				touchStatus = EnvironmentVariables.anglePressDown;
			}else if (newAngle < oldAngle -10){
				touchStatus = EnvironmentVariables.anglePressup;
			}else{
				touchStatus = EnvironmentVariables.noTouchAction;
			}
			oldAngle = newAngle;
			newAngle = 0;
		}
		return touchStatus;
	}


	private static  int calculateLineLength(Point middlePoint, Point startIndex){
		if (middlePoint != null && startIndex != null) {
			return (int) Math.sqrt( Math.pow((middlePoint.x - startIndex.x),2) + Math.pow((middlePoint.x - startIndex.x),2));
		}else{
			return 0;
		}
	}
}

