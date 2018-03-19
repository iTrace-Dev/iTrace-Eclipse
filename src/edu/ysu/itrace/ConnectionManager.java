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
			socket = new Socket("localhost", 8080);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			timer.schedule(new TimerTask(){

				@Override
				public void run() {
					
					try {
						if(reader.ready()){
							data = reader.readLine();
							eventBroker.post("SocketData", data);
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
}