package edu.ysu.itrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JWindow;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import edu.ysu.itrace.GazeCursorWindow;

public class ConnectionManager {
	
	private Socket socket;
	private BufferedReader reader;
	private String data = "";
	private IEventBroker eventBroker;
	private JWindow gazeCursorWindow = new GazeCursorWindow();
	public String dirLocation = "";
	
	ConnectionManager(){
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
							
							
							double x = Double.parseDouble(dataSplit[2]);
							double y = Double.parseDouble(dataSplit[3]);
							
							if (Double.isNaN(x) || Double.isNaN(y)) {
								x = -1;
								y = -1;
							} 
							
							long timestamp = Long.parseLong(dataSplit[1]);
							Gaze gaze = new Gaze(x,y,timestamp, dirLocation);
							
							gazeCursorWindow.setLocation((int)x, (int)y);
							eventBroker.post("iTrace/newgaze", gaze);
							
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
