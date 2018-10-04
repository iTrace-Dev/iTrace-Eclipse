package edu.ysu.itrace;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.JWindow;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import edu.ysu.itrace.GazeCursorWindow;
import org.eclipse.swt.graphics.Point;
import edu.ysu.itrace.solvers.XMLGazeExportSolver;

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
	public String dirLocation = "";
	private XMLGazeExportSolver xmlSolver; 
	
	ConnectionManager(){
		xmlSolver = new XMLGazeExportSolver();
		timer = new Timer();
		eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
		try{
			socket = new Socket("localhost", 8008);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			Thread socketReaderThread = new Thread() {

				@Override
				public void run() {
					while(socket.isClosed() == false) {
						try {
							data = reader.readLine();
							if(data == null) {
								continue;
							}
							String[] dataSplit = data.split(",");
							
							if (dataSplit[0].equalsIgnoreCase("session")) {
								String tmp = dataSplit[1];
								dirLocation = tmp;
								continue;
							}
							
							if(dataSplit[2].toLowerCase().contains("nan") || dataSplit[3].toLowerCase().contains("nan")) {
								dataSplit[2] = "-1";
								dataSplit[3] = "-1";
							}
							
							try {
								double x = Double.parseDouble(dataSplit[2]);
								double y = Double.parseDouble(dataSplit[3]);
								
								if (Double.isNaN(x) || Double.isNaN(y)) {
									x = -1;
									y = -1;
								} 
								
								long timestamp = Long.parseLong(dataSplit[1]);
								Gaze gaze = new Gaze(x,x,y,y,0,0,0,0,timestamp, dirLocation);
								
								if (gazeCursorDisplay == true) {
									if (x < 0 || y < 0) continue;
									counter++;
									totalX += x;
									totalY += y;
									if(counter == 10) {
										int avgX = totalX/10;
										int avgY = totalY/10;
										totalX = 0;
										totalY = 0;
										counter = 0;
										gazeCursorWindow.setLocation(avgX, avgY);
									}
								}
								eventBroker.post("iTrace/newgaze", gaze);
							}
							catch(Exception e) {
								e.printStackTrace();
							}	
							
						} catch (IOException e) {
							e.printStackTrace();
						}	
					}
				}				
			};
			socketReaderThread.start();
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
