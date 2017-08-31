package application;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;



public class ObjRecognitionController
{
	// FXML camera button
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive;

	// Image dilation and erotion values
	private int hueLow = 0, hueHigh = 40, saturationLow = 0, saturationHigh = 40, valueLow = 0, valueHigh = 40;


	@FXML
	private void startCamera()
	{
		// set a fixed width for all the image to show and preserve image ratio
		this.imageViewProperties(this.originalFrame, 600);

		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(0);

			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;

				// grab a frame every 30 ms (33 frames/sec)
				Runnable frameGrabber = new Runnable() {

					public void run()
					{
						Image imageToShow = grabFrame();
						originalFrame.setImage(imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 30, TimeUnit.MILLISECONDS);

				// update the button content
				this.cameraButton.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");

			// stop the timer
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}

			// release the camera
			this.capture.release();
		}
	}

	private Image grabFrame()
	{
		// init everything
		Image imageToShow = null;
		Mat frameToEdit = new Mat();

		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frameToEdit);

				// if the frame is not empty, process it
				if (!frameToEdit.empty())
				{
					// init
					Mat blurredImage = new Mat();
					Mat hsvImage = new Mat();
					Mat mask = new Mat();
					Mat morphOutput = new Mat();

					// remove some noise

					Imgproc.blur(frameToEdit, hsvImage, new Size(7, 7));

					// convert the frame to HSV
//					Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

					// get thresholding values from the UI
					// remember: H ranges 0-180, S and V range 0-255
					Scalar minValues = new Scalar(hueLow, saturationLow, valueLow);
					Scalar maxValues = new Scalar(hueHigh, saturationHigh, valueHigh);

					// threshold HSV image to select tennis balls
					Core.inRange(hsvImage, minValues, maxValues, mask);

					// morphological operators
					// dilate with large element, erode with small ones
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

					Imgproc.dilate(mask, morphOutput, dilateElement);
					Imgproc.erode(mask, morphOutput, erodeElement);
					Imgproc.erode(mask, morphOutput, erodeElement);
					Imgproc.dilate(mask, morphOutput, dilateElement);
					Imgproc.erode(mask, morphOutput, erodeElement);

					// Find all the contours
					List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
					Mat hierarchy = new Mat();
					Imgproc.findContours(morphOutput, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

					// Find the largest contour    
					int maxAreaIndex = 0;
					double maxArea = 0;
					List<MatOfPoint> largestContoursList = new ArrayList<MatOfPoint>();
					for (int i = 0; i < contours.size(); i++) {
						if (Imgproc.contourArea(contours.get(i)) > maxArea) {
							maxArea = Imgproc.contourArea(contours.get(i));
							maxAreaIndex = i;
							largestContoursList.clear();
							largestContoursList.add(contours.get(i));
						}
					}

					Mat finalDisplayFrame = new DetectFingers().handDetector(frameToEdit, largestContoursList, maxAreaIndex);

					//	boolean lengthChangeStatus = new EnvironmentValues().checkForLengthChange();
					//System.out.println(""+lengthChangeStatus);

					//	convert the Mat object (OpenCV) to Image (JavaFX)
					if (!finalDisplayFrame.equals(frameToEdit)) {
						imageToShow = mat2Image(finalDisplayFrame);	
					}else{
						imageToShow = mat2Image(frameToEdit);
					}

				}

			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.print("ERROR");
				e.printStackTrace();
			}
		}

		return imageToShow;
	}

	private void imageViewProperties(ImageView image, int dimension)
	{
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}

	private Image mat2Image(Mat frame){
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		//	if (frame !=null) {
		//	if (!frame.empty()) {
		// encode the frame in the buffer, according to the PNG format
		Highgui.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		//	}
		//	}

		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

}