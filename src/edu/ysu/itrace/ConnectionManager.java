package edu.ysu.itrace;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.JWindow;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import edu.ysu.itrace.GazeCursorWindow;
import edu.ysu.itrace.solvers.XMLGazeExportSolver;
import org.eclipse.swt.graphics.Point;

public class ConnectionManager {
	private Socket socket;
	private BufferedReader reader;
	private String data = "";
	private Timer timer;
	private IEventBroker eventBroker;
	private JWindow gazeCursorWindow = new GazeCursorWindow();
	private boolean gazeCursorDisplay = false;
	private int counter = 0;
	private int totalX = 0;
	private int totalY = 0;
	private Point centre = new Point(8,8);
	ConnectionManager(){
		timer = new Timer();
		eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
		try{
			socket = new Socket("localhost", 8008);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			timer.schedule(new TimerTask(){

				@Override
				public void run() {
					
					try {
						if(reader.ready()){
							data = reader.readLine();
							//eventBroker.post("SocketData", data);
							String[] dataSplit = data.split(",");
							if(dataSplit[1] == "-nan(ind)" || dataSplit[2] == "-nan(ind)") {
								dataSplit[1] = "-1";
								dataSplit[2] = "-1";
							}
							try {
								double x = Double.parseDouble(dataSplit[1]);
								double y = Double.parseDouble(dataSplit[2]);
								if (Double.isNaN(x) || Double.isNaN(y)) {
									x = -1;
									y = -1;
								} 
								long timestamp = Long.parseLong(dataSplit[0]);
								Gaze gaze = new Gaze(x,x,y,y,0,0,0,0,timestamp);
								if (gazeCursorDisplay == true) {
									if (x < 0 || y < 0) return;
									counter++;
									totalX += x;
									totalY += y;
									//gazeCursorWindow.setLocation((int)x, (int)y);
									if(counter == 10) {
										int avgX = totalX/10;
										int avgY = totalY/10;
										totalX = 0;
										totalY = 0;
										counter = 0;
										//crosshairWindow.setLocation((int)x, (int)y);
										gazeCursorWindow.setLocation(avgX- centre.x, avgY - centre.y);
									}
								}
								System.out.println(gaze.getX() + " , " + gaze.getY() + " , " + gaze.getTimestamp() );
								eventBroker.post("iTrace/newgaze", gaze);
							}
							catch(Exception e) {
								e.printStackTrace();
							}	//System.out.println(data);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}		
				}
				
			}, 0,10);
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	public void showGazeCursor(boolean display) {
		gazeCursorWindow.setVisible(display);	
		gazeCursorDisplay = display;	
	}
	void endSocketConnection() {
		try {
			socket.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		}
	}