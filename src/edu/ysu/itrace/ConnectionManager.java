package edu.ysu.itrace;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ConnectionManager {
	private Socket socket;
	private BufferedReader reader;
	private String data = "";
	private Timer timer;
	private IEventBroker eventBroker;
	
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
							double x = Double.parseDouble(dataSplit[1]);
							double y = Double.parseDouble(dataSplit[2]);
							long timestamp = Long.parseLong(dataSplit[0]);
							Gaze gaze = new Gaze(x,x,y,y,0,0,0,0,timestamp);
							eventBroker.post("iTrace/newgaze", gaze);
							System.out.println(data);
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
	
	void endSocketConnection() {
		try {
			socket.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		}
	}