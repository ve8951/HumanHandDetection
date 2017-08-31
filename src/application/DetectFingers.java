package application;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

//import com.sun.glass.ui.GestureSupport;

public class DetectFingers {

	int numberOfFingers = 0;
	double iThreshold = 0;

	public Mat handDetector(Mat modifiedFrame, List<MatOfPoint> contours, int maxAreaIndex) {

		if (contours.size() <= 0) {
			return modifiedFrame;
		}

		int boundPos = 0;

		Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));
		Core.rectangle( modifiedFrame, boundRect.tl(), boundRect.br(), new Scalar(255, 255, 255), 2, 8, 0 );

		List<Moments> mu = new ArrayList<Moments>(contours.size());

		int y =0,x = 0;

		// Find center of the palm
		for (int i = 0; i < contours.size(); i++) {

			mu.add(i, Imgproc.moments(contours.get(i), false));
			Moments p = mu.get(i);
			x = (int) (p.get_m10() / p.get_m00());
			y = (int) (p.get_m01() / p.get_m00());
			Core.circle(modifiedFrame, new Point(x, y), 4, new Scalar(255,255,0,255));

		}
		
		//Draw line from center to the boundaries
		Core.line(modifiedFrame, new Point(boundRect.br().x, y), new Point(boundRect.x, y), new Scalar(100, 100, 100),10);
		
		// Fill shallow convexity defects
		MatOfInt hullIndices = new MatOfInt();
		Imgproc.convexHull(contours.get(0), hullIndices,false);
		MatOfPoint convexHull = OpenCVUtil.getNewContourFromIndices(contours.get(0), hullIndices);

		// Draw convex hull as boundary
		OpenCVUtil.drawContour(modifiedFrame, convexHull, new Scalar(100, 160, 100), false);
		Mat frameToReturn = new Mat();

		// Create rectangle around the hand
		double a = boundRect.br().y - boundRect.tl().y;
		a = a * 0.7;
		a = boundRect.tl().y + a;
		Core.rectangle( modifiedFrame, boundRect.tl(), new Point(boundRect.br().x, a), new Scalar(255, 255, 255), 2, 8, 0 );
		MatOfPoint2f pointMat = new MatOfPoint2f();
		
		//Find the curved regions for convexity defects
		Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 25, true);
		contours.set(boundPos, new MatOfPoint(pointMat.toArray()));
		MatOfInt hull = new MatOfInt();
		MatOfInt4 convexDefect = new MatOfInt4();
		Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);

		if (hull.toList().size() > 3) {
			Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos).toArray()), hull, convexDefect);		
			List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
			List<Point> listPo = new LinkedList<Point>();
			for (int j = 0; j < hull.toList().size(); j++) {
				listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
			}

			MatOfPoint e = new MatOfPoint();
			e.fromList(listPo);
			hullPoints.add(e);

			List<Point> listPoDefect = new LinkedList<Point>();
			List<MODELConvexityDefects> convexityData = new ArrayList<MODELConvexityDefects>();

			for (int j = 0; j < convexDefect.toList().size(); j = j+4) {
				Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j+2));

				if(farPoint.y < a){

					MODELConvexityDefects convexityDefects = new MODELConvexityDefects(contours, convexDefect, boundPos, j);

					Core.circle(modifiedFrame, convexityDefects.getStartIndex(), 6, new Scalar(0,154,0)); // Green
					Core.circle(modifiedFrame, convexityDefects.getEndIndex(), 6, new Scalar(0,154,246)); // Orange
					Core.circle(modifiedFrame, convexityDefects.getFarthestPoint(), 6, new Scalar(0,0,255)); // Red
					
					if (j < convexDefect.toList().size()) {
						Core.line(modifiedFrame, convexityDefects.getStartIndex(), new Point(x, y), new Scalar(250,250,250));
					}
					listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j+2)));
					convexityData.add(convexityDefects);
					
					//Call angle finder function
					/*Core.putText(modifiedFrame, ""+getAngle(new Point(x, y),convexityDefects.getStartIndex())
					, convexityDefects.getStartIndex(), // put fingertip / startIndex text
					Core.FONT_HERSHEY_COMPLEX, 0.8, new Scalar(255, 255, 255));
					*/
					
					/*The below value is used in EnvironmentValues class to access if the length
					 of the line between mid and finger tip has changed*/
//					if (j>3 && j<5) {
//						
//						int didMakeTouch = FingerTouchObserver.didMakeTouch(new Point(x, y),
//								convexityDefects.getStartIndex(), 
//								getAngle(new Point(x, y), 
//										convexityDefects.getStartIndex()));
//						
//						if (didMakeTouch == EnvironmentVariables.didMakeTouch) {
//							System.out.println("Touch complete");
//						}
//					}
				}
			}

			/*Finger count is considered as the number of fingertip up
			 * that is the number of start index (green). 
			 */

			int count = 0;
			for (int i = 0; i < convexityData.size(); i++) {
				if (convexityData.get(i).getStartIndex() != null) {
					numberOfFingers = count++;
				}
			}

			/*The below commented code checks for the number of gaps between two fingers
			 * That is, the count of the farthest point
			 * numberOfFingers = listPoDefect.size()-1 ;
			 */			
			if(numberOfFingers > 5) numberOfFingers = 5;

			// Draws all the points from defect data
			MODELConvexityDefects convexityDefects;

			for (int i = 0; i < convexityData.size(); i++) {

				convexityDefects = new MODELConvexityDefects();
				convexityDefects = convexityData.get(i);

				if (convexityDefects.getFarthestPoint().y < (y+y/2)) {

					try {
						//	Robot robot = new Robot();

						if (numberOfFingers == 1) {
							//Move windows mouse
							//robot.mouseMove((int)convexityDefects.getStartIndex().x, (int) convexityDefects.getStartIndex().y);	
						}else if (numberOfFingers == 0) {
							//int mask = InputEvent.BUTTON1_DOWN_MASK;
							//robot.mousePress(mask);
							//robot.mouseRelease(mask);
						}
					} catch(Exception trace){
						trace.printStackTrace();
					}

				}
			}

			frameToReturn = new Mat();
			frameToReturn = modifiedFrame;
		}
		return frameToReturn;
	}
	
	/*Calculate the angle between the middle line and the finger tip line
	 * Centre point eg: (Xc,Yc) and finger point (Xf,Yf) 
	 * Given two vertices the angle between them can be calculated by:
	 * 
	 * 					(Xc*Xf)+(Yc*Yf)
	 * -------------------------------------------------------
	 * sqrt(square(Xc)+square(Yc))*sqrt(square(Xf)+square(Yf))
	 * 
	 */
	public int getAngle(Point center, Point finger) {
	    int angle = (int) Math.toDegrees(Math.atan2(center.y - finger.y, center.x - finger.x));

	    if(angle < 0){
	        angle += 360;
	    }

	    return angle;
	}

}