/********************************************************************************************************************************************************
* @file ConnectionManager.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.Semaphore;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.itrace.preferences.ITracePreferenceConstants;

public class ConnectionManager {
	
	private Socket socket;
	private InputStream inputStream;
	private BufferedReader reader;	
	private String data;
	private IEventBroker eventBroker;
	private boolean dataReady;
	private Gaze currentGaze;
	public String SessionId;
	public String SessionTimestamp;
	public boolean isRecording = false;
	public Semaphore semaphore;
	
	ConnectionManager() {
		eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
		semaphore = new Semaphore(1);
	}
	
	public boolean isDataReady() {
		return dataReady;
	}
	
	public Gaze popCurrentGaze() {
		Gaze result = null;
		
		try {
			semaphore.acquire();
			result = currentGaze;
			currentGaze = null;
			dataReady = false;
			semaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public void startConnection() {
		try{
			int portNumber = ITrace.getDefault().getPreferenceStore().getInt(ITracePreferenceConstants.PREF_SOCKET_PORT_NUMBER);
			socket = new Socket("localhost", portNumber);
			inputStream = socket.getInputStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			
			Thread socketReaderThread = new Thread() {

				@Override
				public void run() {
					while(socket.isClosed() == false) {
						try {
							Thread.yield();
							if(inputStream.available() == 0) {
								continue;
							}
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
							
							semaphore.acquire();
							currentGaze = gaze;
							dataReady = true;
							semaphore.release();							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
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
