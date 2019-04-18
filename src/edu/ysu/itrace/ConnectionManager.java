package edu.ysu.itrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;

import edu.ysu.itrace.preferences.ITracePreferenceConstants;

public class ConnectionManager {
	
	private Socket socket;
	private BufferedReader reader;
	private String data;
	private IEventBroker eventBroker;
	public String SessionId;
	public String SessionTimestamp;
	public boolean isRecording = false;
	
	ConnectionManager() {
		eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
	}
	
	public void startConnection() {
		try{
			int portNumber = ITrace.getDefault().getPreferenceStore().getInt(ITracePreferenceConstants.PREF_SOCKET_PORT_NUMBER);
			socket = new Socket("localhost", portNumber);
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
							
							if (dataSplit[0].equalsIgnoreCase("session_start")) {
								eventBroker.post("iTrace/sessionstart", new String[] {dataSplit[1], dataSplit[2], dataSplit[3]});
								continue;
							}
							
							if(dataSplit[0].equalsIgnoreCase("session_end")) {
								eventBroker.post("iTrace/sessionend", data);
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
							Gaze gaze = new Gaze(x,y,timestamp);
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
	
	void endSocketConnection() {
		try {
			socket.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
